package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.domain.model.commands.CreateDeviceCommandCommand;
import com.claircore.device.domain.model.aggregates.DeviceCommand;
import com.claircore.device.domain.model.queries.GetDeviceCommandByIdForUserQuery;
import com.claircore.device.domain.model.queries.GetLatestDeviceCommandByDeviceForUserQuery;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.application.commandservices.DeviceControlCommandService;
import com.claircore.device.application.queryservices.DeviceCommandQueryService;
import com.claircore.device.interfaces.rest.resources.CreateDeviceCommandRequest;
import com.claircore.device.interfaces.rest.resources.DeviceCommandResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/devices")
@Tag(name = "Device Commands", description = "Device command creation")
public class DeviceCommandController {

    private final DeviceControlCommandService deviceControlCommandService;
    private final DeviceCommandQueryService deviceCommandQueryService;

    public DeviceCommandController(
            DeviceControlCommandService deviceControlCommandService,
            DeviceCommandQueryService deviceCommandQueryService
    ) {
        this.deviceControlCommandService = deviceControlCommandService;
        this.deviceCommandQueryService = deviceCommandQueryService;
    }

    @PostMapping("/{deviceId}/commands")
    @Operation(summary = "Create a command for a device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Command created"),
            @ApiResponse(responseCode = "400", description = "Invalid command request"),
            @ApiResponse(responseCode = "403", description = "Device does not belong to the authenticated user"),
            @ApiResponse(responseCode = "404", description = "Device assignment not found")
    })
    public ResponseEntity<DeviceCommandResponse> createDeviceCommand(
            @PathVariable UUID deviceId,
            @Valid @RequestBody CreateDeviceCommandRequest request
    ) {
        UUID userId = getAuthenticatedUserId();
        DeviceCommand command = deviceControlCommandService.handle(new CreateDeviceCommandCommand(
                deviceId,
                request.type(),
                request.payload(),
                new UserId(userId)
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(command));
    }

    @GetMapping("/{deviceId}/commands/{commandId}")
    @Operation(summary = "Get a device command by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Command found"),
            @ApiResponse(responseCode = "403", description = "Device does not belong to the authenticated user"),
            @ApiResponse(responseCode = "404", description = "Command not found")
    })
    public ResponseEntity<DeviceCommandResponse> getDeviceCommandById(
            @PathVariable UUID deviceId,
            @PathVariable UUID commandId
    ) {
        UUID userId = getAuthenticatedUserId();
        var query = new GetDeviceCommandByIdForUserQuery(deviceId, commandId, new UserId(userId));
        return deviceCommandQueryService.handle(query)
                .map(command -> ResponseEntity.ok(toResponse(command)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{deviceId}/commands/latest")
    @Operation(summary = "Get the latest device command for a device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Latest command returned"),
            @ApiResponse(responseCode = "403", description = "Device does not belong to the authenticated user"),
            @ApiResponse(responseCode = "404", description = "No commands found for device")
    })
    public ResponseEntity<DeviceCommandResponse> getLatestDeviceCommand(
            @PathVariable UUID deviceId
    ) {
        UUID userId = getAuthenticatedUserId();
        var query = new GetLatestDeviceCommandByDeviceForUserQuery(deviceId, new UserId(userId));
        return deviceCommandQueryService.handle(query)
                .map(command -> ResponseEntity.ok(toResponse(command)))
                .orElse(ResponseEntity.notFound().build());
    }

    private UUID getAuthenticatedUserId() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails userDetails)) {
            throw new org.springframework.security.access.AccessDeniedException("User not authenticated");
        }
        return UUID.fromString(userDetails.getUsername());
    }

    private DeviceCommandResponse toResponse(DeviceCommand command) {
        Instant createdAt = command.getCreatedAt() != null
                ? command.getCreatedAt()
                : null;

        return new DeviceCommandResponse(
                command.getId(),
                command.getDeviceId(),
                command.getType(),
                command.getStatus(),
                command.getPayload(),
                command.getSentAt(),
                command.getExecutedAt(),
                command.getFailureReason(),
                createdAt
        );
    }
}
