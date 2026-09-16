package com.claircore.analytics.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Summarised air quality report for one device over one calendar month (premium)")
public record MonthlyReportResponse(
        @Schema(description = "Device UUID") UUID deviceId,
        @Schema(description = "First day of the month (America/Lima)", example = "2026-05-01") LocalDate month,
        DailyReportResponse.MetricStatsView co2,
        DailyReportResponse.MetricStatsView pm2_5,
        DailyReportResponse.MetricStatsView temperature,
        DailyReportResponse.MetricStatsView humidity,
        @Schema(description = "Highest PM2.5 reading of the month", example = "210.0") Double peakPm2_5,
        @Schema(description = "When the PM2.5 peak occurred") Instant peakPm2_5At,
        @Schema(description = "PM2.5 index of the monthly mean concentration", example = "58") Integer averageAqi,
        @Schema(description = "Most frequent AQI category", example = "MODERATE") String dominantAqiCategory,
        @Schema(description = "Share of readings in each AQI category") List<DailyReportResponse.CategoryShare> categoryShares,
        @Schema(description = "Number of raw readings aggregated", example = "84210") long readingCount,
        @Schema(description = "Number of days with data in the month", example = "31") int daysCovered,
        @Schema(description = "AQI change vs the previous month, percent (null if no prior month)", example = "-4.2") Double aqiDeltaPct
) {
    @com.fasterxml.jackson.annotation.JsonProperty("indexLabel")
    public String indexLabel() { return "Indicative PM2.5 index (EPA breakpoints; not NowCast)"; }

}
