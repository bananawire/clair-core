package com.claircore.device.domain.model.commands;

import com.claircore.device.domain.model.valueobjects.UserId;

import java.util.UUID;

/** Deletes a space on behalf of {@code userId}; the service refuses any other owner. */
public record DeleteSpaceCommand(UUID spaceId, UserId userId) {
    public DeleteSpaceCommand {
        if (spaceId == null) {
            throw new IllegalArgumentException("Space ID must not be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
    }
}
