package com.claircore.analytics.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Historical trend chart data for a device")
public record TrendChartResponse(
        @Schema(description = "List of trend data points")
        List<TrendDataPoint> dataPoints
) {
    @com.fasterxml.jackson.annotation.JsonProperty("indexLabel")
    public String indexLabel() { return "Indicative PM2.5 index (EPA breakpoints; not NowCast)"; }

    @Schema(description = "Single trend data point")
    public record TrendDataPoint(
            @Schema(description = "Timestamp of the snapshot", example = "2026-05-23T10:00:00Z")
            Instant timestamp,

            @Schema(description = "AQI value at this point", example = "75")
            Double aqiValue,

            @Schema(description = "Average CO2 at this point", example = "450.0")
            Double co2,

            @Schema(description = "Average PM2.5 at this point", example = "12.0")
            Double pm2_5,

            @Schema(description = "Average temperature at this point", example = "23.5")
            Double temperature,

            @Schema(description = "Average humidity at this point", example = "52.0")
            Double humidity
    ) {}
}
