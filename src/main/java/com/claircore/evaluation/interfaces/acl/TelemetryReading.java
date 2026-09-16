package com.claircore.evaluation.interfaces.acl;

import java.time.Instant;
import java.util.UUID;

/**
 * Published shape of one raw telemetry reading. Primitives only; no domain types cross here.
 *
 * <p>Analytics builds its daily summaries from these, which is why the reading is published whole
 * rather than pre-aggregated: min, max and the peak timestamp are true extremes, and an average
 * cannot produce them.
 */
public record TelemetryReading(
        UUID deviceId,
        double co2,
        double pm2_5,
        double temperature,
        double humidity,
        Instant recordedAt
) {
}
