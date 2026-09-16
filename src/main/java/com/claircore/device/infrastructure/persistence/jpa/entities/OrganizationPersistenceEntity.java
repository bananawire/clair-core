package com.claircore.device.infrastructure.persistence.jpa.entities;

import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Storage shape of {@code Organization}. The owner column is {@code user_id}: it came from the
 * value object's own {@code @Column}, which the converter does not carry, so it is stated here.
 */
@Entity
@Table(name = "organizations")
public class OrganizationPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "user_id")
    private UserId ownerUserId;

    public OrganizationPersistenceEntity() {
        // JPA, and the persistence assembler
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UserId getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(UserId ownerUserId) { this.ownerUserId = ownerUserId; }
}
