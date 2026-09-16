package com.claircore.device.application.internal.commandservices;

import com.claircore.device.application.internal.outboundservices.acl.ExternalBillingService;
import com.claircore.device.domain.model.commands.*;
import com.claircore.device.domain.model.aggregates.Device;
import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.aggregates.Space;
import com.claircore.device.domain.model.valueobjects.*;
import com.claircore.device.application.commandservices.DeviceCommandService;
import com.claircore.device.domain.repositories.DeviceAssignmentRepository;
import com.claircore.device.domain.repositories.DeviceCommandRepository;
import com.claircore.device.domain.repositories.DeviceRepository;
import com.claircore.device.domain.repositories.OrganizationRepository;
import com.claircore.device.domain.repositories.SpaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DeviceCommandServiceImpl implements DeviceCommandService {

    private static final SecureRandom HARDWARE_ID_RANDOM = new SecureRandom();
    private static final char[] HARDWARE_ID_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int HARDWARE_ID_SUFFIX_LENGTH = 4;

    private final DeviceRepository deviceRepository;
    private final DeviceAssignmentRepository deviceAssignmentRepository;
    private final SpaceRepository spaceRepository;
    private final OrganizationRepository organizationRepository;
    private final ExternalBillingService externalBillingService;
    private final DeviceCommandRepository deviceCommandRepository;

    public DeviceCommandServiceImpl(
            DeviceRepository deviceRepository,
            DeviceAssignmentRepository deviceAssignmentRepository,
            SpaceRepository spaceRepository,
            OrganizationRepository organizationRepository,
            ExternalBillingService externalBillingService,
            DeviceCommandRepository deviceCommandRepository) {
        this.deviceRepository = deviceRepository;
        this.deviceAssignmentRepository = deviceAssignmentRepository;
        this.spaceRepository = spaceRepository;
        this.organizationRepository = organizationRepository;
        this.externalBillingService = externalBillingService;
        this.deviceCommandRepository = deviceCommandRepository;
    }

    @Override
    @Transactional
    public List<Device> handle(SeedDevicesCommand command) {
        List<String> expectedSerialNumbers = new ArrayList<>();
        for (int i = 1; i <= command.count(); i++) {
            expectedSerialNumbers.add("SN-" + String.format("%04d", i));
        }

        // Optimized: Single query to find all existing devices by serial number.
        List<String> existingSerialNumbers = deviceRepository.findAllBySerialNumberIn(expectedSerialNumbers)
                .stream()
                .map(Device::getSerialNumber)
                .toList();

        List<Device> seeded = new ArrayList<>();
        for (String serialNumber : expectedSerialNumbers) {
            if (existingSerialNumbers.contains(serialNumber)) {
                continue;
            }

            // Factory inventory hardware IDs must be random and unique.
            String hardwareId = generateUniqueHardwareId();

            Device device = new Device(
                serialNumber,
                "Sensor " + serialNumber.substring(3),
                new HardwareId(hardwareId),
                ApiKey.generate(),
                new DeviceType("air-quality-v1")
            );
            seeded.add(deviceRepository.save(device));
        }
        return seeded;
    }

    @Override
    @Transactional
    public List<Device> handle(ImportDevicesCommand command) {
        List<Device> imported = new ArrayList<>();
        for (var record : command.records()) {
            if (deviceRepository.findBySerialNumber(record.serialNumber()).isPresent()
                    || deviceRepository.existsByHardwareId(record.hardwareId())) {
                continue;
            }
            Device saved = deviceRepository.save(new Device(
                    record.serialNumber(),
                    record.name(),
                    new HardwareId(record.hardwareId()),
                    new ApiKey(record.apiKey()),
                    new DeviceType("air-quality-v1")));
            imported.add(saved);
        }
        return imported;
    }

    private String generateUniqueHardwareId() {
        // Very low collision probability, but we still guard uniqueness via the repository.
        for (int attempt = 0; attempt < 50; attempt++) {
            String candidate = "CLAIR-" + randomSuffix();
            if (!deviceRepository.existsByHardwareId(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate a unique hardware id after multiple attempts");
    }

    private String randomSuffix() {
        char[] buf = new char[HARDWARE_ID_SUFFIX_LENGTH];
        for (int i = 0; i < HARDWARE_ID_SUFFIX_LENGTH; i++) {
            buf[i] = HARDWARE_ID_ALPHABET[HARDWARE_ID_RANDOM.nextInt(HARDWARE_ID_ALPHABET.length)];
        }
        return new String(buf);
    }

    @Override
    @Transactional
    public DeviceAssignment handle(PairDeviceCommand command) {
        // The device row is the only thing two first pairings share; locking it serializes them so
        // the loser sees the winner's assignment instead of racing the unique constraint.
        Device device = deviceRepository
            .findByHardwareIdForUpdate(command.hardwareId())
            .orElseThrow(() -> new IllegalArgumentException("Device not registered in factory inventory"));

        Optional<DeviceAssignment> existingAssignment = deviceAssignmentRepository.findByDeviceIdForUpdate(device.getId());
        if (existingAssignment.isPresent()) {
            DeviceAssignment assignment = existingAssignment.get();
            // An unclaimed assignment (still waiting for the user to scan the QR) is safe to return:
            // the edge just retried pairing before the claim flow finished. A claimed assignment
            // means the device is already paired to someone and a fresh pairing is an error.
            if (assignment.getOwnerUserId() != null) {
                throw new IllegalStateException("Device is already paired");
            }
            return assignment;
        }

        return deviceAssignmentRepository.save(new DeviceAssignment(device.getId(), ClaimToken.generate()));
    }

    @Override
    @Transactional
    public DeviceAssignment handle(ClaimDeviceCommand command) {
        Space space = spaceRepository
            .findById(command.spaceId())
            .orElseThrow(() -> new IllegalArgumentException("Space not found"));

        if (!space.getOwnerUserId().equals(command.userId())) {
            throw new AccessDeniedException("Space does not belong to user");
        }

        DeviceAssignment assignment = deviceAssignmentRepository
            .findByClaimTokenForUpdate(command.claimToken())
            .orElseThrow(() -> new IllegalArgumentException("Invalid claim token"));

        if (assignment.getOwnerUserId() != null && !assignment.getOwnerUserId().equals(command.userId())) {
            throw new AccessDeniedException("Device assignment belongs to another user");
        }

        // Quota is enforced at the owner boundary, under a lock, before the token is consumed: a
        // failed claim rolls back and leaves the token usable.

        deviceAssignmentRepository.lockOwnerQuotaBoundary(command.userId());

        long owned = deviceAssignmentRepository.countByOwnerUserId(command.userId());

        int maxAllowed = externalBillingService.getMaxDevices(command.userId().userId());

        if (owned >= maxAllowed) {
            throw new IllegalStateException(
                "Cannot claim device. User has " + owned + " devices, max allowed is " + maxAllowed);
        }


        assignment.claimToSpace(command.spaceId(), command.userId());
        return deviceAssignmentRepository.save(assignment);
    }

    @Override
    @Transactional
    public void handle(ResetDeviceAssignmentCommand command) {
        DeviceAssignment assignment = deviceAssignmentRepository
            .findByDeviceIdForUpdate(command.deviceId())
            .orElseThrow(() -> new IllegalArgumentException("Device assignment not found"));

        if (assignment.getOwnerUserId() == null || !assignment.getOwnerUserId().equals(command.userId())) {
            throw new AccessDeniedException("Device does not belong to user");
        }

        // Reset/unlink is not a decommission. Keep the device active and cached.
        Device device = requireDevice(assignment.getDeviceId());
        device.resetNameToFactoryDefault();
        deviceRepository.save(device);

        // Whatever was queued for the old owner is void; the edge learns the same from the roster's
        // assignment id and drops its own cached copies.
        deviceCommandRepository.expireOutstandingByAssignmentId(assignment.getId());

        deviceAssignmentRepository.deleteById(assignment.getId());
        Instant previousWatermark = assignment.getUpdatedAt();
        if (previousWatermark == null || device.getUpdatedAt().isAfter(previousWatermark)) {
            previousWatermark = device.getUpdatedAt();
        }
        deviceRepository.advanceRosterWatermark(device.getId(), previousWatermark);
    }

    @Override
    @Transactional
    public void handle(UpdateDeviceNameCommand command) {
        DeviceAssignment assignment = deviceAssignmentRepository
            .findByDeviceIdForUpdate(command.deviceId())
            .orElseThrow(() -> new IllegalArgumentException("Device assignment not found"));

        if (assignment.getOwnerUserId() == null || !assignment.getOwnerUserId().equals(command.userId())) {
            throw new AccessDeniedException("Device does not belong to user");
        }

        Device device = requireDevice(assignment.getDeviceId());
        device.updateName(command.name());
        deviceRepository.save(device);
    }

    @Override
    public Optional<Device> findById(UUID id) {
        return deviceRepository.findById(id);
    }

    @Override
    public Optional<Device> findBySerialNumber(String serialNumber) {
        return deviceRepository.findBySerialNumber(serialNumber);
    }

    @Override
    public Optional<Device> findByHardwareId(String hardwareId) {
        return deviceRepository.findByHardwareId(hardwareId);
    }

    @Override
    public Optional<Device> findByApiKey(String apiKey) {
        return deviceRepository.findByApiKey(apiKey);
    }

    @Override
    public List<Device> findBySpaceId(UUID spaceId) {
        // Never run an unbounded query; callers needing more should use the paged query API.
        var deviceIds = deviceAssignmentRepository.findBySpaceId(spaceId, 0, 1000).items().stream()
            .map(DeviceAssignment::getDeviceId)
            .toList();
        return deviceRepository.findAllById(deviceIds);
    }

    @Override
    public long countBySpaceId(UUID spaceId) {
        return deviceAssignmentRepository.countBySpaceId(spaceId);
    }

    private Device requireDevice(UUID deviceId) {
        return deviceRepository.findById(deviceId)
            .orElseThrow(() -> new IllegalArgumentException("Device not found"));
    }
}
