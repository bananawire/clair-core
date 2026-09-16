package com.claircore.device.domain.model.queries;

import com.claircore.device.domain.model.valueobjects.UserId;

import java.util.UUID;

/** A space as seen by its owner; anyone else sees nothing. */
public record GetSpaceByIdForUserQuery(UUID spaceId, UserId userId) {
    public GetSpaceByIdForUserQuery {
        if (spaceId == null) throw new IllegalArgumentException("Space ID must not be null");
        if (userId == null) throw new IllegalArgumentException("User ID must not be null");
    }
}
