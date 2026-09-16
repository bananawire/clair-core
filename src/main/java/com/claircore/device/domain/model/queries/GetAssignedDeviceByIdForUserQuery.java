package com.claircore.device.domain.model.queries;

import com.claircore.device.domain.model.valueobjects.UserId;

import java.util.UUID;

/** A claimed device as seen by its owner; anyone else sees nothing. */
public record GetAssignedDeviceByIdForUserQuery(UUID deviceId, UserId userId) {
    public GetAssignedDeviceByIdForUserQuery {
        if (deviceId == null) throw new IllegalArgumentException("Device ID must not be null");
        if (userId == null) throw new IllegalArgumentException("User ID must not be null");
    }
}
