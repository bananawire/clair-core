package com.claircore.device.infrastructure.persistence.jpa.assemblers;

import com.claircore.device.domain.model.aggregates.Space;
import com.claircore.device.infrastructure.persistence.jpa.entities.SpacePersistenceEntity;

public final class SpacePersistenceAssembler {

    private SpacePersistenceAssembler() {
    }

    public static Space toDomainFromPersistence(SpacePersistenceEntity entity) {
        if (entity == null) return null;
        return Space.reconstitute(
                entity.getId(), entity.getName(), entity.getOrganizationId(), entity.getOwnerUserId(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public static SpacePersistenceEntity toPersistenceFromDomain(Space space) {
        if (space == null) return null;
        var entity = new SpacePersistenceEntity();
        entity.setId(space.getId());
        entity.setName(space.getName());
        entity.setOrganizationId(space.getOrganizationId());
        entity.setOwnerUserId(space.getOwnerUserId());
        // Null for a space that has never been written; that marks the entity as new.
        entity.setCreatedAt(space.getCreatedAt());
        entity.setUpdatedAt(space.getUpdatedAt());
        return entity;
    }
}
