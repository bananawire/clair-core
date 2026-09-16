package com.claircore.device.application.internal.queryservices;

import com.claircore.device.domain.model.queries.*;
import org.springframework.security.access.AccessDeniedException;
import com.claircore.device.domain.model.aggregates.Device;
import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.aggregates.Organization;
import com.claircore.device.domain.model.aggregates.Space;
import com.claircore.device.domain.model.queries.*;
import com.claircore.device.application.queryservices.DeviceQueryService;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.domain.repositories.DeviceAssignmentRepository;
import com.claircore.device.domain.repositories.DeviceRepository;
import com.claircore.device.domain.repositories.OrganizationRepository;
import com.claircore.device.domain.repositories.SpaceRepository;
import com.claircore.shared.domain.model.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DeviceQueryServiceImpl implements DeviceQueryService {

    private final OrganizationRepository organizationRepository;
    private final SpaceRepository spaceRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceAssignmentRepository deviceAssignmentRepository;

    public DeviceQueryServiceImpl(
            OrganizationRepository organizationRepository,
            SpaceRepository spaceRepository,
            DeviceRepository deviceRepository,
            DeviceAssignmentRepository deviceAssignmentRepository) {
        this.organizationRepository = organizationRepository;
        this.spaceRepository = spaceRepository;
        this.deviceRepository = deviceRepository;
        this.deviceAssignmentRepository = deviceAssignmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Organization> handle(GetOrganizationByIdQuery query) {
        return organizationRepository.findById(query.organizationId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Organization> handle(GetOrganizationsByOwnerQuery query) {
        return organizationRepository.findByOwnerUserId(query.ownerUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Space> handle(GetSpaceByIdQuery query) {
        return spaceRepository.findById(query.spaceId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Space> handle(GetSpaceByIdForUserQuery query) {
        // Another user's space reads as absent: a 404 leaks nothing about its existence.
        return spaceRepository.findById(query.spaceId())
                .filter(space -> query.userId().equals(space.getOwnerUserId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Space> handle(GetSpacesByOrganizationForUserQuery query) {
        Organization organization = organizationRepository.findById(query.organizationId())
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        if (!query.userId().equals(organization.getOwnerUserId())) {
            throw new AccessDeniedException("Organization does not belong to user");
        }
        return spaceRepository.findByOrganizationId(query.organizationId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Organization> handle(GetOrganizationByIdForUserQuery query) {
        return organizationRepository.findById(query.organizationId())
                .filter(organization -> query.userId().equals(organization.getOwnerUserId()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AssignedDevice> handle(GetDevicesBySpaceForUserQuery query) {
        if (!spaceRepository.existsByIdAndOwnerUserId(query.spaceId(), query.userId())) {
            throw new AccessDeniedException("Space does not belong to user");
        }
        return handle(new GetDevicesBySpaceQuery(query.spaceId(), query.page(), query.size()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AssignedDevice> handle(GetAssignedDeviceByIdForUserQuery query) {
        return findAssignedDeviceByDeviceId(query.deviceId())
                .filter(assigned -> query.userId().equals(assigned.assignment().getOwnerUserId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Space> handle(GetSpacesByOrganizationQuery query) {
        return spaceRepository.findByOrganizationId(query.organizationId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Device> handle(GetDeviceByIdQuery query) {
        return deviceRepository.findById(query.deviceId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Device> handle(GetDeviceBySerialNumberQuery query) {
        return deviceRepository.findBySerialNumber(query.serialNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Device> handle(GetDeviceByHardwareIdQuery query) {
        return deviceRepository.findByHardwareId(query.hardwareId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Device> handle(GetDeviceByApiKeyQuery query) {
        return deviceRepository.findByApiKey(query.apiKey());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AssignedDevice> handle(GetDevicesBySpaceQuery query) {
        int page = query.page() != null ? query.page() : 0;
        int size = query.size() != null ? query.size() : 20;
        var assignments = deviceAssignmentRepository.findBySpaceId(query.spaceId(), page, size);

        // One lookup for the page, not one per row.
        var devices = deviceRepository
                .findAllById(assignments.items().stream().map(DeviceAssignment::getDeviceId).distinct().toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(Device::getId, device -> device));

        return new PageResult<>(
                assignments.items().stream()
                        .filter(assignment -> devices.containsKey(assignment.getDeviceId()))
                        .map(assignment -> new AssignedDevice(assignment, devices.get(assignment.getDeviceId())))
                        .toList(),
                assignments.page(), assignments.size(), assignments.total());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AssignedDevice> findAssignedDeviceByDeviceId(UUID deviceId) {
        return deviceAssignmentRepository.findByDeviceId(deviceId)
                .flatMap(assignment -> deviceRepository.findById(assignment.getDeviceId())
                        .map(device -> new AssignedDevice(assignment, device)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Device> handle(GetProvisionedDevicesQuery query) {
        int cappedLimit = Math.max(1, Math.min(query.limit(), 5000));
        return deviceRepository.findProvisionedDevices(null, null, cappedLimit).items().stream()
                .map(row -> deviceRepository.findById(row.deviceId()))
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findSpaceIdByDeviceId(UUID deviceId) {
        return deviceAssignmentRepository.findByDeviceId(deviceId)
                .map(DeviceAssignment::getSpaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDeviceOwnedByUser(UUID deviceId, UUID userId) {
        if (deviceId == null || userId == null) return false;
        return deviceAssignmentRepository.existsByDeviceIdAndOwnerUserId(deviceId, new UserId(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findOwnerIdByDeviceId(UUID deviceId) {
        return deviceAssignmentRepository.findByDeviceId(deviceId)
                .map(DeviceAssignment::getOwnerUserId)
                .map(UserId::userId);
    }


    @Override
    @Transactional(readOnly = true)
    public boolean isSpaceOwnedByUser(UUID spaceId, UUID userId) {
        if (spaceId == null || userId == null) return false;
        return spaceRepository.existsByIdAndOwnerUserId(spaceId, new UserId(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findDeviceIdsByOwnerId(UUID ownerUserId) {
        if (ownerUserId == null) return List.of();
        return deviceAssignmentRepository.findDeviceIdsByOwnerUserId(new UserId(ownerUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> findDeviceNamesByDeviceIds(List<UUID> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) return Map.of();
        return deviceRepository.findAllById(deviceIds)
                .stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Device::getId,
                        Device::getName,
                        (a, b) -> a
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> findHardwareIdsByDeviceIds(List<UUID> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) return Map.of();
        return deviceRepository.findAllById(deviceIds)
                .stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Device::getId,
                        device -> device.getHardwareId().value(),
                        (a, b) -> a
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> findSpaceNamesBySpaceIds(List<UUID> spaceIds) {
        if (spaceIds == null || spaceIds.isEmpty()) return Map.of();
        return spaceIds.stream()
                .map(spaceRepository::findById)
                .flatMap(Optional::stream)
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Space::getId,
                        Space::getName,
                        (a, b) -> a
                ));
    }
    @Override
    @Transactional(readOnly = true)
    public Optional<java.time.Instant> findActivatedAtByDeviceId(UUID deviceId) {
        return deviceAssignmentRepository.findByDeviceId(deviceId)
                .filter(assignment -> assignment.getOwnerUserId() != null)
                .map(DeviceAssignment::getActivatedAt);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeviceAssignment> findAssignmentByDeviceId(UUID deviceId) {
        return deviceAssignmentRepository.findByDeviceId(deviceId);
    }

    @Override
    @Transactional
    public Optional<DeviceAssignment> findAssignmentByDeviceIdForUpdate(UUID deviceId) {
        return deviceAssignmentRepository.findByDeviceIdForUpdate(deviceId);
    }

    @Override
    @Transactional
    public void updatePresence(UUID deviceId, com.claircore.device.domain.model.valueobjects.DeviceStatus status, java.time.Instant occurredAt) {
        DeviceAssignment assignment = deviceAssignmentRepository.findByDeviceIdForUpdate(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device assignment not found for " + deviceId));
        assignment.updatePresence(status, occurredAt != null ? occurredAt : java.time.Instant.now());
        deviceAssignmentRepository.save(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Device> handle(GetDevicesForTelemetryQuery query) {
        return deviceRepository.findDevicesForTelemetry(query.limit(), query.includeDeleted());
    }
}

