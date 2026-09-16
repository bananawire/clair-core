package com.claircore.device.infrastructure.persistence.jpa.assemblers;

import com.claircore.device.domain.model.aggregates.DeviceCommand;
import com.claircore.device.infrastructure.persistence.jpa.entities.DeviceCommandPersistenceEntity;

public final class DeviceCommandPersistenceAssembler {

    private DeviceCommandPersistenceAssembler() {
    }

    public static DeviceCommand toDomainFromPersistence(DeviceCommandPersistenceEntity entity) {
        if (entity == null) return null;
        return DeviceCommand.reconstitute(
                entity.getId(),
                entity.getDeviceId(),
                entity.getAssignmentId(),
                entity.getType(),
                entity.getStatus(),
                entity.getPayload(),
                entity.getSentAt(),
                entity.getExecutedAt(),
                entity.getFailureReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static DeviceCommandPersistenceEntity toPersistenceFromDomain(DeviceCommand command) {
        if (command == null) return null;
        var entity = new DeviceCommandPersistenceEntity();
        entity.setId(command.getId());
        entity.setDeviceId(command.getDeviceId());
        entity.setAssignmentId(command.getAssignmentId());
        entity.setType(command.getType());
        entity.setStatus(command.getStatus());
        entity.setPayload(command.getPayload());
        entity.setSentAt(command.getSentAt());
        entity.setExecutedAt(command.getExecutedAt());
        entity.setFailureReason(command.getFailureReason());
        // Null for a command that has never been written; that is what marks the entity as new.
        entity.setCreatedAt(command.getCreatedAt());
        entity.setUpdatedAt(command.getUpdatedAt());
        return entity;
    }
}
