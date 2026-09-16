package com.claircore.device.domain.model.queries;

import com.claircore.device.domain.model.valueobjects.UserId;

import java.util.UUID;

/** An organization as seen by its owner; anyone else sees nothing. */
public record GetOrganizationByIdForUserQuery(UUID organizationId, UserId userId) {
    public GetOrganizationByIdForUserQuery {
        if (organizationId == null) throw new IllegalArgumentException("Organization ID must not be null");
        if (userId == null) throw new IllegalArgumentException("User ID must not be null");
    }
}
