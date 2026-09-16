package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.domain.model.commands.RemoveDeviceThresholdCommand;
import com.claircore.device.domain.model.commands.WriteDeviceThresholdCommand;
import com.claircore.device.domain.model.queries.GetDeviceThresholdsByDeviceQuery;
import com.claircore.device.domain.model.valueobjects.DeviceMetricThresholdConfiguration;
import com.claircore.device.domain.model.valueobjects.MetricThreshold;
import com.claircore.device.domain.model.valueobjects.DeviceThresholdWriteIntent;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.application.commandservices.DeviceThresholdCommandService;
import com.claircore.device.application.queryservices.DeviceThresholdQueryService;
import com.claircore.device.interfaces.rest.resources.DeviceThresholdResponse;
import com.claircore.device.interfaces.rest.resources.UpdateDeviceThresholdRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/devices/{deviceId}/thresholds")
@Tag(name = "Device Thresholds", description = "Device threshold configuration endpoints")
public class DeviceThresholdController {

    private final DeviceThresholdCommandService deviceThresholdCommandService;
    private final DeviceThresholdQueryService deviceThresholdQueryService;

    public DeviceThresholdController(
            DeviceThresholdCommandService deviceThresholdCommandService,
            DeviceThresholdQueryService deviceThresholdQueryService) {
        this.deviceThresholdCommandService = deviceThresholdCommandService;
        this.deviceThresholdQueryService = deviceThresholdQueryService;
    }

    @GetMapping
    @Operation(summary = "Get all thresholds for a device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thresholds returned"),
            @ApiResponse(responseCode = "403", description = "Device does not belong to user"),
            @ApiResponse(responseCode = "404", description = "Device not found")
    })
    public ResponseEntity<List<DeviceThresholdResponse>> getThresholds(
            @Parameter(description = "Device ID") @PathVariable UUID deviceId) {

        UUID userId = getAuthenticatedUserId();
        var query = new GetDeviceThresholdsByDeviceQuery(deviceId, new UserId(userId));
        List<DeviceMetricThresholdConfiguration> thresholds = deviceThresholdQueryService.handle(query);

        List<DeviceThresholdResponse> responses = thresholds.stream()
                .map(t -> DeviceThresholdResponse.from(t, deviceId))
                .toList();

        return ResponseEntity.ok(responses);
    }

    @PostMapping
    @Operation(summary = "Create a new threshold for a specific metric")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Threshold created"),
            @ApiResponse(responseCode = "400", description = "Invalid request or threshold already exists"),
            @ApiResponse(responseCode = "403", description = "Device does not belong to user"),
            @ApiResponse(responseCode = "404", description = "Device not found")
    })
    public ResponseEntity<DeviceThresholdResponse> createThreshold(
            @Parameter(description = "Device ID") @PathVariable UUID deviceId,
            @Valid @RequestBody UpdateDeviceThresholdRequest request) {

        UUID userId = getAuthenticatedUserId();
        var command = new WriteDeviceThresholdCommand(
                deviceId,
                new UserId(userId),
                request.metric(),
                request.value(),
                request.enabled(),
                DeviceThresholdWriteIntent.CREATE
        );

        DeviceMetricThresholdConfiguration threshold = deviceThresholdCommandService.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DeviceThresholdResponse.from(threshold, deviceId));
    }

    @PutMapping
    @Operation(summary = "Update an existing threshold for a specific metric")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Threshold updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request or threshold does not exist"),
            @ApiResponse(responseCode = "403", description = "Device does not belong to user"),
            @ApiResponse(responseCode = "404", description = "Device not found")
    })
    public ResponseEntity<DeviceThresholdResponse> updateThreshold(
            @Parameter(description = "Device ID") @PathVariable UUID deviceId,
            @Valid @RequestBody UpdateDeviceThresholdRequest request) {

        UUID userId = getAuthenticatedUserId();
        var command = new WriteDeviceThresholdCommand(
                deviceId,
                new UserId(userId),
                request.metric(),
                request.value(),
                request.enabled(),
                DeviceThresholdWriteIntent.UPDATE
        );

        DeviceMetricThresholdConfiguration threshold = deviceThresholdCommandService.handle(command);
        return ResponseEntity.ok(DeviceThresholdResponse.from(threshold, deviceId));
    }

    @DeleteMapping("/{metric}")
    @Operation(summary = "Remove a threshold for a specific metric")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Threshold removed"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "403", description = "Device does not belong to user"),
            @ApiResponse(responseCode = "404", description = "Threshold or device not found")
    })
    public ResponseEntity<Void> removeThreshold(
            @Parameter(description = "Device ID") @PathVariable UUID deviceId,
            @Parameter(description = "Metric type") @PathVariable MetricThreshold metric) {

        UUID userId = getAuthenticatedUserId();
        var command = new RemoveDeviceThresholdCommand(deviceId, new UserId(userId), metric);
        deviceThresholdCommandService.handle(command);
        return ResponseEntity.noContent().build();
    }

    private UUID getAuthenticatedUserId() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails userDetails)) {
            throw new org.springframework.security.access.AccessDeniedException("User not authenticated");
        }
        return UUID.fromString(userDetails.getUsername());
    }
}
