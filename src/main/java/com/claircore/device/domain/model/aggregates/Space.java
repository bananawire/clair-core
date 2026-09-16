package com.claircore.device.domain.model.aggregates;

import com.claircore.device.domain.model.valueobjects.UserId;

import java.time.Instant;
import java.util.UUID;

/** A room or area inside an organization; devices are assigned to one. */
public class Space {

    private final UUID id;
    private String name;
    private final UUID organizationId;
    private final UserId ownerUserId;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Space(UUID id, String name, UUID organizationId, UserId ownerUserId,
                  Instant createdAt, Instant updatedAt) {
        if (id == null) {
            throw new IllegalArgumentException("Id must not be null");
        }
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID must not be null");
        }
        this.id = id;
        this.name = name;
        this.organizationId = organizationId;
        this.ownerUserId = ownerUserId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Space(String name, UUID organizationId, UserId ownerUserId) {
        this(UUID.randomUUID(), name, organizationId, ownerUserId, null, null);
    }

    /** Rebuilds a space already in storage; only a persistence assembler should call this. */
    public static Space reconstitute(
            UUID id, String name, UUID organizationId, UserId ownerUserId,
            Instant createdAt, Instant updatedAt) {
        return new Space(id, name, organizationId, ownerUserId, createdAt, updatedAt);
    }

    public void updateName(String name) {
        this.name = name;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public UUID getOrganizationId() { return organizationId; }
    public UserId getOwnerUserId() { return ownerUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
