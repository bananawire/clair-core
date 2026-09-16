package com.claircore.evaluation.domain.model.valueobjects;

import java.time.Instant;
import java.util.UUID;

/**
 * One device reading reduced to the four metrics analytics summarises, plus when it was recorded.
 * A read model, produced by the repository, never stored.
 */
public record DeviceReading(
        UUID deviceId,
        Double co2,
        Double pm2_5,
        Double temperature,
        Double humidity,
        Instant recordedAt
) {
    public DeviceReading {
        if (deviceId == null) throw new IllegalArgumentException("Device ID must not be null");
        if (recordedAt == null) throw new IllegalArgumentException("recordedAt must not be null");
    }
}
