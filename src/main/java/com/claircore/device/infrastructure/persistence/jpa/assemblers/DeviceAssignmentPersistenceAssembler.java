package com.claircore.device.infrastructure.persistence.jpa.assemblers;

import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.infrastructure.persistence.jpa.entities.DeviceAssignmentPersistenceEntity;

public final class DeviceAssignmentPersistenceAssembler {

    private DeviceAssignmentPersistenceAssembler() {
    }

    public static DeviceAssignment toDomainFromPersistence(DeviceAssignmentPersistenceEntity entity) {
        if (entity == null) return null;
        return DeviceAssignment.reconstitute(
                entity.getId(),
                entity.getDeviceId(),
                entity.getOwnerUserId(),
                entity.getSpaceId(),
                entity.getStatus(),
                // Copied out of the entity: the aggregate must not hold JPA's tracked collection.
                entity.getConfiguration(),
                entity.getClaimToken(),
                entity.getActivatedAt(),
                entity.getLastSeenAt(),
                entity.getPresenceAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static DeviceAssignmentPersistenceEntity toPersistenceFromDomain(DeviceAssignment assignment) {
        if (assignment == null) return null;
        var entity = new DeviceAssignmentPersistenceEntity();
        entity.setId(assignment.getId());
        entity.setDeviceId(assignment.getDeviceId());
        entity.setOwnerUserId(assignment.getOwnerUserId());
        entity.setSpaceId(assignment.getSpaceId());
        entity.setStatus(assignment.getStatus());
        // Every threshold the device was configured with lives in here. Dropping it loses them
        // silently: nothing else reads the column, so no other test would fail.
        entity.setConfiguration(assignment.getConfiguration());
        entity.setClaimToken(assignment.getClaimToken());
        entity.setActivatedAt(assignment.getActivatedAt());
        entity.setLastSeenAt(assignment.getLastSeenAt());
        entity.setPresenceAt(assignment.getPresenceAt());
        // Null for an assignment that has never been written; that is what marks the entity as new.
        entity.setCreatedAt(assignment.getCreatedAt());
        entity.setUpdatedAt(assignment.getUpdatedAt());
        return entity;
    }
}
