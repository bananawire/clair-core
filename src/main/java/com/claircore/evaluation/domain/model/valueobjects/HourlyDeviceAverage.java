package com.claircore.evaluation.domain.model.valueobjects;

import java.util.UUID;

/**
 * One row of the hourly telemetry aggregation: a device and its averages over the window.
 * A read model, produced by the repository, never stored.
 */
public record HourlyDeviceAverage(
        UUID deviceId,
        Double averageCo2,
        Double averagePm25,
        Double averageTemperature,
        Double averageHumidity
) {
    public HourlyDeviceAverage {
        if (deviceId == null) throw new IllegalArgumentException("Device ID must not be null");
    }
}
