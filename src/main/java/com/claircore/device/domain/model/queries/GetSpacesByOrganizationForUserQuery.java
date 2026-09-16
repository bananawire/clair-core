package com.claircore.device.domain.model.queries;

import com.claircore.device.domain.model.valueobjects.UserId;

import java.util.UUID;

/** The spaces of an organization, readable only by the organization's owner. */
public record GetSpacesByOrganizationForUserQuery(UUID organizationId, UserId userId) {
    public GetSpacesByOrganizationForUserQuery {
        if (organizationId == null) throw new IllegalArgumentException("Organization ID must not be null");
        if (userId == null) throw new IllegalArgumentException("User ID must not be null");
    }
}
