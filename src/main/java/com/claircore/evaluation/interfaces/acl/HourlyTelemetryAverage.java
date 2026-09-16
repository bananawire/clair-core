package com.claircore.evaluation.interfaces.acl;

import java.util.UUID;

/** Published shape of one hourly aggregation row. Primitives only; no domain types cross here. */
public record HourlyTelemetryAverage(
        UUID deviceId,
        double averageCo2,
        double averagePm25,
        double averageTemperature,
        double averageHumidity
) {
}
