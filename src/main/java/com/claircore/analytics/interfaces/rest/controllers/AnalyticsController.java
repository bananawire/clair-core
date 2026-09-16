package com.claircore.analytics.interfaces.rest.controllers;

import com.claircore.analytics.domain.model.queries.GetHistoricalTrendQuery;
import com.claircore.analytics.domain.model.queries.GetDashboardMetricsQuery;
import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.domain.model.valueobjects.TrendPeriod;
import com.claircore.analytics.application.queryservices.KpiDashboardMetricsQueryService;
import com.claircore.analytics.application.queryservices.KpiHistoricalTrendQueryService;
import com.claircore.shared.domain.exceptions.ResourceNotFoundException;
import com.claircore.analytics.interfaces.rest.resources.DashboardMetricsResponse;
import com.claircore.analytics.interfaces.rest.resources.TrendChartResponse;
import com.claircore.analytics.interfaces.rest.transform.AnalyticsResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.claircore.analytics.interfaces.rest.sse.AnalyticsSseService;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "KPI and trend analytics endpoints")
public class AnalyticsController {

    private final KpiDashboardMetricsQueryService kpiDashboardMetricsQueryService;
    private final KpiHistoricalTrendQueryService kpiHistoricalTrendQueryService;
    private final AnalyticsSseService analyticsSseService;

    public AnalyticsController(
            KpiDashboardMetricsQueryService kpiDashboardMetricsQueryService,
            KpiHistoricalTrendQueryService kpiHistoricalTrendQueryService,
            AnalyticsSseService analyticsSseService
    ) {
        this.kpiDashboardMetricsQueryService = kpiDashboardMetricsQueryService;
        this.kpiHistoricalTrendQueryService = kpiHistoricalTrendQueryService;
        this.analyticsSseService = analyticsSseService;
    }

    @GetMapping("/devices/{deviceId}/live")
    @Operation(summary = "Get live dashboard KPI metrics for a device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dashboard metrics returned"),
            @ApiResponse(responseCode = "404", description = "No data available for device")
    })
    public ResponseEntity<DashboardMetricsResponse> getLiveMetrics(
            @Parameter(description = "Device UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable @NotNull UUID deviceId
    ) {
        var query = new GetDashboardMetricsQuery(new DeviceId(deviceId), TrendPeriod.LIVE, null, null);
        return kpiDashboardMetricsQueryService.handle(query)
                .map(AnalyticsResourceFromEntityAssembler::toDashboardResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Device with ID %s has no recent live telemetry data; it might be turned off or disconnected.".formatted(deviceId)));
    }

    @GetMapping(value = "/devices/{deviceId}/live/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream live telemetry updates for a device using Server-Sent Events (SSE)")
    @ApiResponse(responseCode = "200", description = "SSE stream established")
    public SseEmitter streamLiveMetrics(
            @Parameter(description = "Device UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable @NotNull UUID deviceId
    ) {
        return analyticsSseService.registerClient(deviceId);
    }

    @GetMapping("/devices/{deviceId}/historical")
    @Operation(summary = "Get historical dashboard KPI metrics for a device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dashboard metrics returned"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "404", description = "No data available for device")
    })
    public ResponseEntity<DashboardMetricsResponse> getHistoricalMetrics(
            @Parameter(description = "Device UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable @NotNull UUID deviceId,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @PastOrPresent Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @PastOrPresent Instant endDate
    ) {
        if (startDate != null && endDate != null && !endDate.isAfter(startDate)) {
            return ResponseEntity.badRequest().build();
        }
        var query = new GetDashboardMetricsQuery(new DeviceId(deviceId), parsePeriod(period), startDate, endDate);
        return kpiDashboardMetricsQueryService.handle(query)
                .map(AnalyticsResourceFromEntityAssembler::toDashboardResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("No telemetry data available for device with ID %s in the requested period.".formatted(deviceId)));
    }

    @GetMapping("/devices/{deviceId}/trends")
    @Operation(summary = "Get historical trend chart data for a device")
    @ApiResponse(responseCode = "200", description = "Trend data returned")
    public ResponseEntity<TrendChartResponse> getTrends(
            @Parameter(description = "Device UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable @NotNull UUID deviceId,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @PastOrPresent Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @PastOrPresent Instant endDate,
            @RequestParam(required = false, defaultValue = "100") Integer limit
    ) {
        if (startDate != null && endDate != null && !endDate.isAfter(startDate)) {
            return ResponseEntity.badRequest().build();
        }
        var query = new GetHistoricalTrendQuery(new DeviceId(deviceId), parsePeriod(period), startDate, endDate, limit);
        var points = kpiHistoricalTrendQueryService.handle(query);
        
        return ResponseEntity.ok(AnalyticsResourceFromEntityAssembler.toTrendChartResponse(points));
    }

    /**
     * Case-insensitive, as it has always been on the wire. An unrecognised name is now rejected
     * rather than silently read as {@code DAY}, which is the one behaviour this endpoint changes.
     */
    private static TrendPeriod parsePeriod(String period) {
        if (period == null || period.isBlank()) {
            return null;
        }
        try {
            return TrendPeriod.valueOf(period.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown period: " + period);
        }
    }
}
