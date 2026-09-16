package com.claircore.device.domain.model.commands;

import com.claircore.device.domain.model.valueobjects.UserId;

import java.util.UUID;

/** Renames an organization on behalf of {@code userId}; the service refuses any other owner. */
public record UpdateOrganizationNameCommand(
    UUID organizationId,
    String name,
    UserId userId
) {
    public UpdateOrganizationNameCommand {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be null or blank");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
    }
}
