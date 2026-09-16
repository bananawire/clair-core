package com.claircore.device.domain.model.commands;

import com.claircore.device.domain.model.valueobjects.UserId;

import java.util.UUID;

/** Deletes an organization on behalf of {@code userId}; the service refuses any other owner. */
public record DeleteOrganizationCommand(UUID organizationId, UserId userId) {
    public DeleteOrganizationCommand {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID must not be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
    }
}
