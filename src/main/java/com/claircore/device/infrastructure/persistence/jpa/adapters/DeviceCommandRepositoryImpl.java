package com.claircore.device.infrastructure.persistence.jpa.adapters;

import com.claircore.device.domain.model.aggregates.DeviceCommand;
import com.claircore.device.domain.model.valueobjects.DeviceCommandStatus;
import com.claircore.device.domain.model.valueobjects.HardwareId;
import com.claircore.device.domain.repositories.DeviceCommandRepository;
import com.claircore.device.infrastructure.persistence.jpa.assemblers.DeviceCommandPersistenceAssembler;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceCommandPersistenceRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DevicePersistenceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DeviceCommandRepositoryImpl implements DeviceCommandRepository {

    private final DeviceCommandPersistenceRepository commandPersistenceRepository;
    private final DevicePersistenceRepository devicePersistenceRepository;

    public DeviceCommandRepositoryImpl(
            DeviceCommandPersistenceRepository commandPersistenceRepository,
            DevicePersistenceRepository devicePersistenceRepository) {
        this.commandPersistenceRepository = commandPersistenceRepository;
        this.devicePersistenceRepository = devicePersistenceRepository;
    }

    @Override
    public DeviceCommand save(DeviceCommand command) {
        var saved = commandPersistenceRepository.save(
                DeviceCommandPersistenceAssembler.toPersistenceFromDomain(command));
        return DeviceCommandPersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public Optional<DeviceCommand> findById(UUID id) {
        return commandPersistenceRepository.findById(id)
                .map(DeviceCommandPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<DeviceCommand> findByIdForAcknowledgement(UUID commandId) {
        return commandPersistenceRepository.findByIdForAcknowledgement(commandId)
                .map(DeviceCommandPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<DeviceCommand> findByDeviceIdAndCommandId(UUID deviceId, UUID commandId) {
        return commandPersistenceRepository.findByDeviceIdAndId(deviceId, commandId)
                .map(DeviceCommandPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<DeviceCommand> findLatestByDeviceId(UUID deviceId) {
        return commandPersistenceRepository.findFirstByDeviceIdOrderByCreatedAtDesc(deviceId)
                .map(DeviceCommandPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<DeviceCommand> findByStatusForDispatch(DeviceCommandStatus status, int limit) {
        return commandPersistenceRepository.findByStatusOrderByCreatedAtAsc(status, PageRequest.of(0, limit))
                .stream().map(DeviceCommandPersistenceAssembler::toDomainFromPersistence).toList();
    }

    @Override
    public List<DeviceCommand> findPendingForEdge(Instant since, Instant leaseCutoff, int limit) {
        return commandPersistenceRepository.findPendingForEdge(since, leaseCutoff, PageRequest.of(0, limit))
                .stream().map(DeviceCommandPersistenceAssembler::toDomainFromPersistence).toList();
    }

    /**
     * The hardware id is resolved to a device id first rather than joined in the query. An unknown
     * unit has no commands, which is the same answer the join gave.
     */
    @Override
    public List<DeviceCommand> findPendingForEdgeByHardware(
            String hardwareId, Instant since, Instant leaseCutoff, int limit) {
        return devicePersistenceRepository.findByHardwareId(new HardwareId(hardwareId))
                .map(device -> commandPersistenceRepository
                        .findPendingForEdgeByDevice(device.getId(), since, leaseCutoff, PageRequest.of(0, limit))
                        .stream().map(DeviceCommandPersistenceAssembler::toDomainFromPersistence).toList())
                .orElseGet(List::of);
    }

    @Override
    public int claimForEdge(UUID commandId, Instant leaseCutoff, Instant claimedAt) {
        return commandPersistenceRepository.claimForEdge(commandId, leaseCutoff, claimedAt);
    }

    @Override
    public int expireOutstandingByAssignmentId(UUID assignmentId) {
        return commandPersistenceRepository.expireOutstandingByAssignmentId(assignmentId);
    }
}
