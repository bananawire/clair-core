package com.claircore.analytics.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Live KPI dashboard metrics for a device")
public record DashboardMetricsResponse(
        @Schema(description = "Indicative PM2.5 index of mean concentration (0–500 display cap)", example = "75")
        Integer aqiValue,

        @Schema(description = "PM2.5 index category", example = "MODERATE")
        String aqiCategory,

        @Schema(description = "Moving average CO2 in ppm", example = "450.0")
        Double averageCo2,

        @Schema(description = "Moving average PM2.5 in µg/m³", example = "12.0")
        Double averagePm2_5,

        @Schema(description = "Moving average temperature in Celsius", example = "23.5")
        Double averageTemperature,

        @Schema(description = "Moving average humidity in percent", example = "52.0")
        Double averageHumidity,

        @Schema(description = "CO2 trend delta percentage", example = "-5.2")
        Double co2DeltaPercentage,

        @Schema(description = "PM2.5 trend delta percentage", example = "2.1")
        Double pm2_5DeltaPercentage,

        @Schema(description = "Temperature trend delta percentage", example = "0.5")
        Double temperatureDeltaPercentage,

        @Schema(description = "Humidity trend delta percentage", example = "-1.3")
        Double humidityDeltaPercentage,

        @Schema(description = "Latest measurement time for live results; calculation time for historical results", example = "2026-05-23T10:00:00Z")
        Instant calculatedAt
) {
    @com.fasterxml.jackson.annotation.JsonProperty("indexLabel")
    public String indexLabel() { return "Indicative PM2.5 index (EPA breakpoints; not NowCast)"; }

}
