package com.claircore.evaluation.domain.model.queries;

import java.time.Instant;
import java.util.UUID;

/** The newest reading of a device at or after {@code visibleSince}; null means no lower bound. */
public record GetLatestEvaluationByDeviceQuery(
        UUID deviceId,
        Instant visibleSince
) {
    public GetLatestEvaluationByDeviceQuery {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
    }

    public GetLatestEvaluationByDeviceQuery(UUID deviceId) {
        this(deviceId, null);
    }
}
