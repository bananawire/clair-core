package com.claircore.device.domain.model.queries;

import com.claircore.device.domain.model.valueobjects.UserId;

import java.util.UUID;

/** The device page of a space, readable only by the space's owner. */
public record GetDevicesBySpaceForUserQuery(UUID spaceId, Integer page, Integer size, UserId userId) {
    public GetDevicesBySpaceForUserQuery {
        if (spaceId == null) throw new IllegalArgumentException("Space ID must not be null");
        if (page != null && page < 0) throw new IllegalArgumentException("Page must be non-negative");
        if (size != null && size < 1) throw new IllegalArgumentException("Size must be at least 1");
        if (userId == null) throw new IllegalArgumentException("User ID must not be null");
    }
}
