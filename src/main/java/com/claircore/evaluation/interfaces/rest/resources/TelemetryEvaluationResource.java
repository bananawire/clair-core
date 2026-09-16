package com.claircore.evaluation.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Response representing a stored telemetry record")
public record TelemetryEvaluationResource(
        @Schema(description = "Evaluation ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID id,

        @Schema(description = "Device ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID deviceId,

        @Schema(description = "Stable reading UUID reused on retries", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID readingId,

        @Schema(description = "System uptime in seconds", example = "20")
        Long uptime,

        @Schema(description = "Air quality sensor data")
        AirQualityResource airQuality,

        @Schema(description = "Particulate matter sensor data")
        ParticulateMatterResource particulateMatter,

        @Schema(description = "WiFi connectivity status")
        ConnectivityResource connectivity,

        @Schema(description = "Device location")
        LocationResource location,

        @Schema(description = "Device health status percentage", example = "100")
        Integer healthStatus,

        @Schema(description = "Overall device status", example = "Optimal")
        String status,

        @Schema(description = "When the reading was recorded", example = "2026-05-16T22:30:00Z")
        Instant recordedAt,

        @Schema(description = "When the record was created", example = "2026-05-16T22:30:05Z")
        Instant createdAt
) {
    @com.fasterxml.jackson.annotation.JsonProperty("measuredAt")
    public Instant measuredAt() { return recordedAt; }

    @com.fasterxml.jackson.annotation.JsonProperty("receivedAt")
    public Instant receivedAt() { return createdAt; }

    @Schema(description = "Air quality sensor data")
    public record AirQualityResource(
            Double co2,
            Double temperature,
            Double humidity
    ) {}

    @Schema(description = "Particulate matter sensor data")
    public record ParticulateMatterResource(
            Double pm1_0,
            Double pm2_5,
            Double pm10
    ) {}

    @Schema(description = "WiFi connectivity status")
    public record ConnectivityResource(
            String status,
            String network,
            Integer signalStrength
    ) {}

    @Schema(description = "Location data")
    public record LocationResource(
            String country
    ) {}
}
