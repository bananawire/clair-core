package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.domain.model.commands.ClaimDeviceCommand;
import com.claircore.device.domain.model.commands.PairDeviceCommand;
import com.claircore.device.domain.model.commands.ResetDeviceAssignmentCommand;
import com.claircore.device.domain.model.commands.UpdateDeviceNameCommand;
import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.queries.GetAssignedDeviceByIdForUserQuery;
import com.claircore.device.domain.model.queries.GetDevicesBySpaceForUserQuery;
import com.claircore.device.domain.model.queries.GetDeviceStatusByDeviceIdForUserQuery;
import com.claircore.device.domain.model.valueobjects.DeviceMetricThresholdConfiguration;
import com.claircore.device.domain.model.valueobjects.MetricThreshold;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.application.commandservices.DeviceCommandService;
import com.claircore.device.application.queryservices.DeviceQueryService;
import com.claircore.device.application.queryservices.DeviceStatusQueryService;
import com.claircore.device.interfaces.rest.resources.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.claircore.shared.domain.model.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/v1/devices")
@Tag(name = "Devices", description = "Device pairing and management endpoints")
public class DeviceController {

    private final DeviceCommandService deviceCommandService;
    private final DeviceQueryService deviceQueryService;
    private final DeviceStatusQueryService deviceStatusQueryService;
    private final ObjectMapper objectMapper;

    public DeviceController(
            DeviceCommandService deviceCommandService,
            DeviceQueryService deviceQueryService,
            DeviceStatusQueryService deviceStatusQueryService,
            ObjectMapper objectMapper) {
        this.deviceCommandService = deviceCommandService;
        this.deviceQueryService = deviceQueryService;
        this.deviceStatusQueryService = deviceStatusQueryService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/pair")
    @Operation(summary = "Pair a physical device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pairing started and claim token issued"),
            @ApiResponse(responseCode = "400", description = "Device not registered in factory inventory")
    })
    public ResponseEntity<DevicePairingResource> pairDevice(@Valid @RequestBody PairDeviceRequest request) {
        var command = new PairDeviceCommand(request.hardwareId());
        DeviceAssignment assignment = deviceCommandService.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new DevicePairingResource(
                        assignment.getDeviceId(),
                        assignment.getClaimToken() != null ? assignment.getClaimToken().value() : null
                )
        );
    }

    @PostMapping("/claim")
    @Operation(summary = "Claim a device into a user-owned space")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Device claimed and assigned to the requested space"),
            @ApiResponse(responseCode = "400", description = "Invalid claim token, missing fields, or device already claimed"),
            @ApiResponse(responseCode = "403", description = "The target space does not belong to the authenticated user")
    })
    public ResponseEntity<DeviceResponse> claimDevice(
            @Valid @RequestBody ClaimDeviceRequest request) {

        UUID userId = getAuthenticatedUserId();
        var command = new ClaimDeviceCommand(
                request.claimToken(),
                request.spaceId(),
                new UserId(userId)
        );

        DeviceAssignment assignment = deviceCommandService.handle(command);
        return deviceQueryService.findAssignedDeviceByDeviceId(assignment.getDeviceId())
                .map(assigned -> ResponseEntity.ok(toResponse(assigned)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Get devices by space with pagination")
    public ResponseEntity<Page<DeviceResponse>> getDevices(
            @RequestParam UUID spaceId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        var query = new GetDevicesBySpaceForUserQuery(spaceId, page, size, new UserId(getAuthenticatedUserId()));
        PageResult<DeviceQueryService.AssignedDevice> assigned = deviceQueryService.handle(query);
        // The port speaks PageResult; the body stays a Spring Data page so the JSON envelope
        // clients already consume is unchanged.
        return ResponseEntity.ok(new PageImpl<>(
                assigned.items().stream().map(this::toResponse).toList(),
                PageRequest.of(assigned.page(), assigned.size()),
                assigned.total()));
    }

    @GetMapping("/{deviceId}")
    @Operation(summary = "Get device by ID")
    public ResponseEntity<DeviceResponse> getDevice(@PathVariable UUID deviceId) {
        return deviceQueryService.handle(new GetAssignedDeviceByIdForUserQuery(deviceId, new UserId(getAuthenticatedUserId())))
                .map(assigned -> ResponseEntity.ok(toResponse(assigned)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{deviceId}/status")
    @Operation(summary = "Get current device status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status returned"),
            @ApiResponse(responseCode = "403", description = "The device does not belong to the authenticated user"),
            @ApiResponse(responseCode = "404", description = "Device assignment not found")
    })
    public ResponseEntity<DeviceStatusResponse> getDeviceStatus(
            @PathVariable UUID deviceId
    ) {
        UUID userId = getAuthenticatedUserId();
        var query = new GetDeviceStatusByDeviceIdForUserQuery(deviceId, new UserId(userId));

        return deviceStatusQueryService.handle(query)
                .map(assignment -> ResponseEntity.ok(new DeviceStatusResponse(
                        assignment.getDeviceId(),
                        assignment.getStatus(),
                        assignment.getLastSeenAt()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{deviceId}")
    @Operation(summary = "Reset a device assignment for reconfiguration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Device assignment reset and space cleared"),
            @ApiResponse(responseCode = "400", description = "Device not found or invalid request"),
            @ApiResponse(responseCode = "403", description = "The device does not belong to the authenticated user")
    })
    public ResponseEntity<Void> deleteDevice(@PathVariable UUID deviceId) {
        UUID userId = getAuthenticatedUserId();
        deviceCommandService.handle(new ResetDeviceAssignmentCommand(deviceId, new UserId(userId)));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping({"/{deviceId}/name", "/{deviceId}"})
    @Operation(summary = "Update device display name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Device name updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "403", description = "The device does not belong to the authenticated user")
    })
    public ResponseEntity<Void> updateDeviceName(
            @PathVariable UUID deviceId,
            @Valid @RequestBody UpdateDeviceNameRequest request) {

        UUID userId = getAuthenticatedUserId();
        deviceCommandService.handle(new UpdateDeviceNameCommand(deviceId, request.name(), new UserId(userId)));
        return ResponseEntity.ok().build();
    }

    private UUID getAuthenticatedUserId() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails userDetails)) {
            throw new org.springframework.security.access.AccessDeniedException("User not authenticated");
        }
        return UUID.fromString(userDetails.getUsername());
    }

    private DeviceResponse toResponse(DeviceQueryService.AssignedDevice assigned) {
        var assignment = assigned.assignment();
        var device = assigned.device();
        return new DeviceResponse(
                device.getId(),
                device.getSerialNumber(),
                device.getName(),
                assignment.getStatus(),
                assignment.getSpaceId(),
                assignment.getOwnerUserId() != null ? assignment.getOwnerUserId().userId() : null,
                assignment.getConfiguration(),
                buildThresholdResponses(assignment, device.getId()),
                device.getHardwareId().value(),
                device.getDeviceType().value(),
                assignment.getActivatedAt(),
                assignment.getLastSeenAt(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt()
        );
    }

    private List<DeviceThresholdResponse> buildThresholdResponses(DeviceAssignment assignment, UUID deviceId) {
        return Stream.of(MetricThreshold.values())
                .map(metric -> {
                    String key = "threshold." + metric.name();
                    return assignment.findConfigurationValue(key)
                            .flatMap(json -> deserialize(json))
                            .map(config -> DeviceThresholdResponse.from(config, deviceId))
                            .orElse(null);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private Optional<DeviceMetricThresholdConfiguration> deserialize(String json) {
        try {
            return Optional.of(objectMapper.readValue(json, DeviceMetricThresholdConfiguration.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
