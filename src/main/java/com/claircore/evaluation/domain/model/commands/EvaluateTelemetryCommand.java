package com.claircore.evaluation.domain.model.commands;

import com.claircore.evaluation.domain.model.valueobjects.*;

import java.time.Instant;
import java.util.UUID;

public record EvaluateTelemetryCommand(
        DeviceId deviceId,
        UUID readingId,
        Long uptime,
        AirQuality airQuality,
        ParticulateMatter particulateMatter,
        Connectivity connectivity,
        Location location,
        Integer healthStatus,
        String status,
        Instant recordedAt
) {
    public EvaluateTelemetryCommand {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (readingId == null) {
            throw new IllegalArgumentException("readingId must not be null");
        }
        if (uptime == null || uptime < 0) {
            throw new IllegalArgumentException("uptime must not be null or negative");
        }
        if (airQuality == null) {
            throw new IllegalArgumentException("airQuality must not be null");
        }
        if (particulateMatter == null) {
            throw new IllegalArgumentException("particulateMatter must not be null");
        }
        if (connectivity == null) {
            throw new IllegalArgumentException("connectivity must not be null");
        }
        if (location == null) {
            throw new IllegalArgumentException("location must not be null");
        }
        if (healthStatus == null || healthStatus < 0 || healthStatus > 100) {
            throw new IllegalArgumentException("healthStatus must be between 0 and 100");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status must not be null or blank");
        }
        if (recordedAt != null) recordedAt = recordedAt.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        if (recordedAt == null) {
            throw new IllegalArgumentException("recordedAt must not be null");
        }
    }
}
