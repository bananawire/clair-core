package com.claircore.analytics.interfaces.rest.controllers;

import com.claircore.analytics.domain.model.queries.GetOverviewDashboardQuery;
import com.claircore.analytics.application.queryservices.OverviewDashboardQueryService;
import com.claircore.analytics.interfaces.rest.resources.AnalyticsOverviewResponse;
import com.claircore.analytics.interfaces.rest.transform.AnalyticsOverviewResourceFromEntityAssembler;
import com.claircore.shared.interfaces.rest.security.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics Overview", description = "Aggregated overview dashboard endpoints")
@Validated
public class AnalyticsOverviewController {

    private final OverviewDashboardQueryService overviewDashboardQueryService;

    public AnalyticsOverviewController(OverviewDashboardQueryService overviewDashboardQueryService) {
        this.overviewDashboardQueryService = overviewDashboardQueryService;
    }

    @GetMapping("/overview")
    @Operation(summary = "Get overview dashboard for the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Overview returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content)
    })
    public ResponseEntity<AnalyticsOverviewResponse> getOverview(
            @CurrentUserId UUID userId,
            @RequestParam(required = false, defaultValue = "50") @Min(1) @Max(200) Integer deviceLimitPerSpace,
            @RequestParam(required = false, defaultValue = "10") @Min(1) @Max(50) Integer alertLimit
    ) {
        var query = new GetOverviewDashboardQuery(userId, deviceLimitPerSpace, alertLimit);
        var snapshot = overviewDashboardQueryService.handle(query);
        return ResponseEntity.ok(AnalyticsOverviewResourceFromEntityAssembler.toResponse(snapshot));
    }
}

