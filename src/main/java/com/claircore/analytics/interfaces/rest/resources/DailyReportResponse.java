package com.claircore.analytics.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Summarised air quality report for one device over one calendar day")
public record DailyReportResponse(
        @Schema(description = "Device UUID") UUID deviceId,
        @Schema(description = "Calendar day (America/Lima)", example = "2026-05-27") LocalDate date,
        MetricStatsView co2,
        MetricStatsView pm2_5,
        MetricStatsView temperature,
        MetricStatsView humidity,
        @Schema(description = "Highest PM2.5 reading of the day", example = "182.0") Double peakPm2_5,
        @Schema(description = "When the PM2.5 peak occurred") Instant peakPm2_5At,
        @Schema(description = "PM2.5 index of the daily mean concentration", example = "62") Integer averageAqi,
        @Schema(description = "Most frequent AQI category", example = "MODERATE") String dominantAqiCategory,
        @Schema(description = "Share of readings in each AQI category") List<CategoryShare> categoryShares,
        @Schema(description = "Number of raw readings aggregated", example = "2873") long readingCount,
        @Schema(description = "AQI change vs the previous day, percent (null if no prior day)", example = "8.1") Double aqiDeltaPct
) {
    @com.fasterxml.jackson.annotation.JsonProperty("indexLabel")
    public String indexLabel() { return "Indicative PM2.5 index (EPA breakpoints; not NowCast)"; }

    @Schema(description = "Average / minimum / maximum of a metric over the period")
    public record MetricStatsView(Double avg, Double min, Double max) {}

    @Schema(description = "Share of samples in each PM category; not elapsed time")
    public record CategoryShare(
            @Schema(example = "MODERATE") String category,
            @Schema(example = "1840") long count,
            @Schema(description = "Percentage of total readings", example = "64.0") double percentage
    ) {}
}
