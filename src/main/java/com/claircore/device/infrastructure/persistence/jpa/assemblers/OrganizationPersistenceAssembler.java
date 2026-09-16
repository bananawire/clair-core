package com.claircore.device.infrastructure.persistence.jpa.assemblers;

import com.claircore.device.domain.model.aggregates.Organization;
import com.claircore.device.infrastructure.persistence.jpa.entities.OrganizationPersistenceEntity;

public final class OrganizationPersistenceAssembler {

    private OrganizationPersistenceAssembler() {
    }

    public static Organization toDomainFromPersistence(OrganizationPersistenceEntity entity) {
        if (entity == null) return null;
        return Organization.reconstitute(
                entity.getId(), entity.getName(), entity.getOwnerUserId(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public static OrganizationPersistenceEntity toPersistenceFromDomain(Organization organization) {
        if (organization == null) return null;
        var entity = new OrganizationPersistenceEntity();
        entity.setId(organization.getId());
        entity.setName(organization.getName());
        entity.setOwnerUserId(organization.getOwnerUserId());
        // Null for an organization that has never been written; that marks the entity as new.
        entity.setCreatedAt(organization.getCreatedAt());
        entity.setUpdatedAt(organization.getUpdatedAt());
        return entity;
    }
}
