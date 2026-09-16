package com.claircore.evaluation.domain.model.queries;

import java.time.Instant;
import java.util.UUID;

/**
 * A page of a device's readings, newest first. {@code visibleSince} hides readings recorded before
 * the current owner claimed the device; null means no lower bound (trusted internal reads only).
 */
public record GetEvaluationsByDeviceQuery(
        UUID deviceId,
        Integer page,
        Integer size,
        Instant visibleSince
) {
    public GetEvaluationsByDeviceQuery {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
    }

    public GetEvaluationsByDeviceQuery(UUID deviceId, Integer page, Integer size) {
        this(deviceId, page, size, null);
    }
}
