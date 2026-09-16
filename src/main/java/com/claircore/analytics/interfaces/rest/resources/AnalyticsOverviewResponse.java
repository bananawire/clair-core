package com.claircore.analytics.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Aggregated overview dashboard for the authenticated user")
public record AnalyticsOverviewResponse(
        @Schema(description = "Aggregated core metrics across all owned devices")
        CoreMetrics core,

        @Schema(description = "Organizations and spaces included in the overview")
        List<OrganizationItem> organizations,

        @Schema(description = "Most recent alerts across all owned devices")
        List<AlertItem> alerts,

        @Schema(description = "When the overview was computed", example = "2026-05-23T10:00:00Z")
        Instant updatedAt
) {
    @com.fasterxml.jackson.annotation.JsonProperty("indexLabel")
    public String indexLabel() { return "Indicative PM2.5 index (EPA breakpoints; not NowCast)"; }

    @Schema(description = "Aggregated metrics across the user's device fleet")
    public record CoreMetrics(
            @Schema(description = "Aggregated AQI value", example = "75", nullable = true)
            Integer aqiValue,

            @Schema(description = "Aggregated AQI category", example = "MODERATE", nullable = true)
            String aqiCategory,

            @Schema(description = "Average CO2 in ppm", example = "450.0", nullable = true)
            Double averageCo2,

            @Schema(description = "Average PM2.5 in µg/m³", example = "12.0", nullable = true)
            Double averagePm2_5,

            @Schema(description = "Average temperature in Celsius", example = "23.5", nullable = true)
            Double averageTemperature,

            @Schema(description = "Average humidity in percent", example = "52.0", nullable = true)
            Double averageHumidity,

            @Schema(description = "CO2 trend delta percentage", example = "-5.2", nullable = true)
            Double co2DeltaPercentage,

            @Schema(description = "PM2.5 trend delta percentage", example = "2.1", nullable = true)
            Double pm2_5DeltaPercentage,

            @Schema(description = "Temperature trend delta percentage", example = "0.5", nullable = true)
            Double temperatureDeltaPercentage,

            @Schema(description = "Humidity trend delta percentage", example = "-1.3", nullable = true)
            Double humidityDeltaPercentage,

            @Schema(description = "Latest recorded time among included device metrics", nullable = true)
            Instant recordedAt,

            @Schema(description = "Organizations included in the overview", example = "2")
            Integer organizationCount,

            @Schema(description = "Spaces included in the overview", example = "4")
            Integer spaceCount,

            @Schema(description = "Unique devices included in the overview", example = "8")
            Integer deviceCount,

            @Schema(description = "Freshness indicator: LIVE, STALE, or NO_DATA", example = "LIVE")
            String dataFreshness
    ) {}

    @Schema(description = "Organization item with its spaces")
    public record OrganizationItem(
            @Schema(description = "Organization ID")
            UUID organizationId,

            @Schema(description = "Organization display name")
            String organizationName,

            @Schema(description = "Spaces under this organization")
            List<SpaceItem> spaces
    ) {}

    @Schema(description = "Space item with summary metrics")
    public record SpaceItem(
            @Schema(description = "Space ID")
            UUID spaceId,

            @Schema(description = "Parent organization ID")
            UUID organizationId,

            @Schema(description = "Space display name")
            String spaceName,

            @Schema(description = "Aggregated AQI value for the space", nullable = true)
            Integer aqiValue,

            @Schema(description = "Aggregated AQI category for the space", nullable = true)
            String aqiCategory,

            @Schema(description = "Latest recorded time among space device metrics", nullable = true)
            Instant recordedAt,

            @Schema(description = "Number of devices included in the space aggregation", example = "3")
            Integer deviceCount,

            @Schema(description = "Freshness indicator for the space", example = "STALE")
            String dataFreshness
    ) {}

    @Schema(description = "Alert item for the overview list")
    public record AlertItem(
            @Schema(description = "Alert ID")
            UUID alertId,

            @Schema(description = "Device ID related to the alert")
            UUID deviceId,

            @Schema(description = "Space ID related to the alert", nullable = true)
            UUID spaceId,

            @Schema(description = "Resolved device name (best-effort)", nullable = true)
            String deviceName,

            @Schema(description = "Resolved space name (best-effort)", nullable = true)
            String spaceName,

            @Schema(description = "Alert message")
            String message,

            @Schema(description = "Alert severity", example = "HIGH")
            String severity,

            @Schema(description = "Alert status", example = "ACTIVE")
            String status,

            @Schema(description = "When the alert occurred", example = "2026-05-23T10:00:00Z")
            Instant occurredAt
    ) {}
}
