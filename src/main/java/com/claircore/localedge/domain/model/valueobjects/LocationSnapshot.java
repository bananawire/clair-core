package com.claircore.localedge.domain.model.valueobjects;

import java.time.Instant;

/**
 * Geographic snapshot bound to a telemetry reading. Only the country is reported (Peru, per the
 * brief); finer fields are intentionally absent so we cannot couple back into the device BC.
 */
public record LocationSnapshot(
        String country,
        Instant observedAt
) {
    public LocationSnapshot {
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("country must not be blank");
        }
        if (observedAt == null) {
            throw new IllegalArgumentException("observedAt must not be null");
        }
    }
}
