package com.claircore.device.domain.model.commands;

import com.claircore.device.domain.model.valueobjects.UserId;

import java.util.UUID;

/** Renames a space on behalf of {@code userId}; the service refuses any other owner. */
public record UpdateSpaceNameCommand(
    UUID spaceId,
    String name,
    UserId userId
) {
    public UpdateSpaceNameCommand {
        if (spaceId == null) {
            throw new IllegalArgumentException("Space ID must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be null or blank");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
    }
}
