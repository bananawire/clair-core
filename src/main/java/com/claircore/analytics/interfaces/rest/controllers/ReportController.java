package com.claircore.analytics.interfaces.rest.controllers;

import com.claircore.analytics.application.internal.outboundservices.acl.ExternalBillingService;
import com.claircore.analytics.application.internal.outboundservices.acl.ExternalDeviceService;
import com.claircore.analytics.domain.model.queries.GetDailyReportQuery;
import com.claircore.analytics.domain.model.queries.GetMonthlyReportQuery;
import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.application.queryservices.DailyReportQueryService;
import com.claircore.analytics.application.queryservices.MonthlyReportQueryService;
import com.claircore.analytics.interfaces.rest.resources.DailyReportResponse;
import com.claircore.analytics.interfaces.rest.resources.MonthlyReportResponse;
import com.claircore.analytics.interfaces.rest.transform.AnalyticsResourceFromEntityAssembler;
import com.claircore.shared.interfaces.rest.security.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Reports", description = "Daily and monthly air quality reports")
public class ReportController {

    private final DailyReportQueryService dailyReportQueryService;
    private final MonthlyReportQueryService monthlyReportQueryService;
    private final ExternalDeviceService externalDeviceService;
    private final ExternalBillingService externalBillingService;

    public ReportController(
            DailyReportQueryService dailyReportQueryService,
            MonthlyReportQueryService monthlyReportQueryService,
            ExternalDeviceService externalDeviceService,
            ExternalBillingService externalBillingService
    ) {
        this.dailyReportQueryService = dailyReportQueryService;
        this.monthlyReportQueryService = monthlyReportQueryService;
        this.externalDeviceService = externalDeviceService;
        this.externalBillingService = externalBillingService;
    }

    @GetMapping("/devices/{deviceId}/reports/daily")
    @Operation(summary = "Get a device's daily report (latest completed day, or a specific date)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report returned"),
            @ApiResponse(responseCode = "403", description = "Device does not belong to user"),
            @ApiResponse(responseCode = "404", description = "No report available for the given day")
    })
    public ResponseEntity<DailyReportResponse> getDailyReport(
            @CurrentUserId UUID userId,
            @Parameter(description = "Device UUID") @PathVariable UUID deviceId,
            @Parameter(description = "Calendar day YYYY-MM-DD; omit for the latest completed day", example = "2026-05-27")
            @RequestParam(required = false) String date
    ) {
        requireDeviceOwnership(deviceId, userId);

        LocalDate parsedDate = date == null ? null : parseDate(date);
        var query = new GetDailyReportQuery(new DeviceId(deviceId), parsedDate);
        return dailyReportQueryService.handle(query)
                .map(AnalyticsResourceFromEntityAssembler::toDailyReportResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/devices/{deviceId}/reports/monthly")
    @Operation(summary = "Get a device's monthly report (premium only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report returned"),
            @ApiResponse(responseCode = "403", description = "Device does not belong to user, or plan does not include monthly reports"),
            @ApiResponse(responseCode = "404", description = "No report available for the given month")
    })
    public ResponseEntity<MonthlyReportResponse> getMonthlyReport(
            @CurrentUserId UUID userId,
            @Parameter(description = "Device UUID") @PathVariable UUID deviceId,
            @Parameter(description = "Month YYYY-MM; omit for the latest completed month", example = "2026-05")
            @RequestParam(required = false) String month
    ) {
        requireDeviceOwnership(deviceId, userId);
        if (!externalBillingService.canAccessMonthlyReports(userId)) {
            throw new AccessDeniedException("Monthly reports require a premium subscription");
        }

        LocalDate parsedMonth = month == null
                ? YearMonth.now().minusMonths(1).atDay(1)
                : parseMonth(month);
        var query = new GetMonthlyReportQuery(new DeviceId(deviceId), parsedMonth);
        return monthlyReportQueryService.handle(query)
                .map(AnalyticsResourceFromEntityAssembler::toMonthlyReportResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private void requireDeviceOwnership(UUID deviceId, UUID userId) {
        if (!externalDeviceService.isDeviceOwnedByUser(deviceId, userId)) {
            throw new AccessDeniedException("Device does not belong to user");
        }
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date, expected YYYY-MM-DD: " + date);
        }
    }

    private LocalDate parseMonth(String month) {
        try {
            return YearMonth.parse(month).atDay(1);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid month, expected YYYY-MM: " + month);
        }
    }
}
