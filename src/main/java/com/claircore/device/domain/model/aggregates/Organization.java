package com.claircore.device.domain.model.aggregates;

import com.claircore.device.domain.model.valueobjects.UserId;

import java.time.Instant;
import java.util.UUID;

/** A tenant boundary: one owner's spaces and, through them, their devices. */
public class Organization {

    private static final int MAX_SPACES = 5;
    private static final int MAX_DEVICES = 10;

    private final UUID id;
    private String name;
    private final UserId ownerUserId;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Organization(UUID id, String name, UserId ownerUserId, Instant createdAt, Instant updatedAt) {
        if (id == null) {
            throw new IllegalArgumentException("Id must not be null");
        }
        this.id = id;
        this.name = name;
        this.ownerUserId = ownerUserId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Organization(String name, UserId ownerUserId) {
        this(UUID.randomUUID(), name, ownerUserId, null, null);
    }

    /** Rebuilds an organization already in storage; only a persistence assembler should call this. */
    public static Organization reconstitute(
            UUID id, String name, UserId ownerUserId, Instant createdAt, Instant updatedAt) {
        return new Organization(id, name, ownerUserId, createdAt, updatedAt);
    }

    public int getMaxSpaces() {
        return MAX_SPACES;
    }

    public int getMaxDevices() {
        return MAX_DEVICES;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public UserId getOwnerUserId() { return ownerUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
