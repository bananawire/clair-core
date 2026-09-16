package com.claircore.device.infrastructure.persistence.jpa.entities;

import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/** Storage shape of {@code Space}; the owner column is {@code user_id}, as on organizations. */
@Entity
@Table(name = "spaces")
public class SpacePersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "user_id")
    private UserId ownerUserId;

    public SpacePersistenceEntity() {
        // JPA, and the persistence assembler
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }

    public UserId getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(UserId ownerUserId) { this.ownerUserId = ownerUserId; }
}
