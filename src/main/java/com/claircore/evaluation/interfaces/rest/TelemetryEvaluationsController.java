package com.claircore.evaluation.interfaces.rest;

import com.claircore.evaluation.application.internal.outboundservices.acl.ExternalDeviceService;
import com.claircore.evaluation.domain.model.commands.EvaluateTelemetryCommand;
import com.claircore.evaluation.domain.model.aggregates.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.queries.GetEvaluationsByDeviceQuery;
import com.claircore.evaluation.domain.model.queries.GetLatestEvaluationByDeviceQuery;
import com.claircore.evaluation.domain.model.valueobjects.*;
import com.claircore.evaluation.application.commandservices.TelemetryEvaluationCommandService;
import com.claircore.evaluation.application.queryservices.TelemetryEvaluationQueryService;
import com.claircore.evaluation.interfaces.rest.resources.EvaluateTelemetryResource;
import com.claircore.evaluation.interfaces.rest.resources.TelemetryEvaluationResource;
import com.claircore.evaluation.interfaces.rest.transform.TelemetryEvaluationResourceFromEntityAssembler;
import com.claircore.shared.interfaces.rest.security.CurrentUserId;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

import java.time.format.DateTimeParseException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/evaluations")
@Tag(name = "Evaluations", description = "Telemetry storage endpoints")
public class TelemetryEvaluationsController {

    private static final int MAX_BATCH_SIZE = 10;

    private final TelemetryEvaluationQueryService telemetryEvaluationQueryService;
    private final TelemetryEvaluationCommandService telemetryEvaluationCommandService;
    private final ExternalDeviceService externalDeviceService;

    public TelemetryEvaluationsController(
            TelemetryEvaluationQueryService telemetryEvaluationQueryService,
            TelemetryEvaluationCommandService telemetryEvaluationCommandService,
            ExternalDeviceService externalDeviceService
    ) {
        this.telemetryEvaluationQueryService = telemetryEvaluationQueryService;
        this.telemetryEvaluationCommandService = telemetryEvaluationCommandService;
        this.externalDeviceService = externalDeviceService;
    }

    @PostMapping("/telemetry")
    @Operation(summary = "Store telemetry record from edge device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Record stored successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "404", description = "Device not found")
    })
    public ResponseEntity<TelemetryEvaluationResource> evaluateTelemetry(@Valid @RequestBody EvaluateTelemetryResource request) {
        UUID deviceId = resolveDeviceId(request.deviceId()).orElse(null);
        if (deviceId == null) return ResponseEntity.notFound().build();

        UUID readingId;
        try {
            readingId = UUID.fromString(request.readingId());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("readingId must be a UUID", e);
        }

        Long uptimeSeconds;
        try {
            uptimeSeconds = Long.parseLong(request.uptime());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid uptime format: " + request.uptime(), e);
        }

        Instant recordedAt;
        try {
            recordedAt = Instant.parse(request.created_at());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid created_at format: " + request.created_at(), e);
        }

        var command = new EvaluateTelemetryCommand(
                new DeviceId(deviceId),
                readingId,
                uptimeSeconds,
                new AirQuality(request.airQuality().co2(), request.airQuality().temperature(), request.airQuality().humidity()),
                new ParticulateMatter(request.particulateMatter().pm1_0(), request.particulateMatter().pm2_5(), request.particulateMatter().pm10()),
                new Connectivity(request.connectivity().status(), request.connectivity().network(), request.connectivity().signalStrength()),
                new Location(request.location().country()),
                request.healthStatus(),
                request.status(),
                recordedAt
        );

        TelemetryEvaluation evaluation = telemetryEvaluationCommandService.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(TelemetryEvaluationResourceFromEntityAssembler.toResourceFromEntity(evaluation));
    }

    @PostMapping("/telemetry/batch")
    public ResponseEntity<java.util.Map<String, Object>> evaluateTelemetryBatch(@RequestBody JsonNode body) {
        JsonNode records = body == null ? null : body.get("records");
        if (records == null || !records.isArray() || records.size() > MAX_BATCH_SIZE) {
            return ResponseEntity.badRequest().build();
        }
        var results = new java.util.ArrayList<java.util.Map<String, Object>>();
        for (JsonNode record : records) {
            String ref = record.path("client_ref").asText("");
            try {
                ResponseEntity<TelemetryEvaluationResource> response = evaluateTelemetry(batchRequest(record));
                if (response.getStatusCode().is2xxSuccessful()) results.add(java.util.Map.of("client_ref", ref, "status", "CREATED"));
                else results.add(java.util.Map.of("client_ref", ref, "status", "ERROR", "reason", "DEVICE_NOT_FOUND"));
            }
            catch (IllegalArgumentException ex) { results.add(java.util.Map.of("client_ref", ref, "status", "ERROR", "reason", "VALIDATION_ERROR")); }
        }
        return ResponseEntity.ok(java.util.Map.of("results", results));
    }

    private EvaluateTelemetryResource batchRequest(JsonNode r) {
        String[] required = {"device_id", "reading_id", "uptime_seconds", "co2", "temperature", "humidity", "pm1_0", "pm2_5", "pm10", "wifi_status", "health_status", "status", "occurred_at"};
        for (String name : required) if (!r.has(name) || r.get(name).isNull() || (r.get(name).isTextual() && r.get(name).asText().isBlank())) throw new IllegalArgumentException("Missing " + name);
        requireText(r, "device_id"); requireText(r, "reading_id"); requireText(r, "wifi_status"); requireText(r, "status"); requireText(r, "occurred_at");
        try { Instant.parse(r.get("occurred_at").asText()); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException("Invalid timestamp format", ex); }
        requireNumber(r, "uptime_seconds"); requireNumber(r, "co2"); requireNumber(r, "temperature"); requireNumber(r, "humidity"); requireNumber(r, "health_status");
        requireNumber(r, "pm1_0"); requireNumber(r, "pm2_5"); requireNumber(r, "pm10");
        requireInteger(r, "health_status");
        if (!r.get("uptime_seconds").isIntegralNumber() || !r.get("uptime_seconds").canConvertToLong()) throw new IllegalArgumentException("Invalid uptime_seconds");
        if (r.get("uptime_seconds").asLong() < 0 || r.get("health_status").asInt() < 0 || r.get("health_status").asInt() > 100) throw new IllegalArgumentException("Invalid range");
        // Construct the existing request shape so singular and batch paths share the same command handling.
        return new EvaluateTelemetryResource(r.get("device_id").asText(), r.get("reading_id").asText(),
                Long.toString(r.get("uptime_seconds").asLong()),
                new EvaluateTelemetryResource.AirQualityResource(r.get("co2").doubleValue(), r.get("temperature").doubleValue(), r.get("humidity").doubleValue()),
                new EvaluateTelemetryResource.ParticulateMatterResource(r.get("pm1_0").doubleValue(), r.get("pm2_5").doubleValue(), r.get("pm10").doubleValue()),
                new EvaluateTelemetryResource.ConnectivityResource(r.get("wifi_status").asText(), r.path("network_name").asText(null), r.path("signal_strength").isNumber() ? r.get("signal_strength").intValue() : null),
                new EvaluateTelemetryResource.LocationResource(r.path("country").asText(null)), r.get("health_status").intValue(),
                r.get("status").asText(), r.get("occurred_at").asText());
    }

    private void requireText(JsonNode r, String name) { if (!r.get(name).isTextual() || r.get(name).asText().isBlank()) throw new IllegalArgumentException("Invalid " + name); }
    private void requireNumber(JsonNode r, String name) { if (!r.get(name).isNumber() || !Double.isFinite(r.get(name).asDouble())) throw new IllegalArgumentException("Invalid " + name); }
    private void requireInteger(JsonNode r, String name) { if (!r.get(name).isIntegralNumber() || !r.get(name).canConvertToInt()) throw new IllegalArgumentException("Invalid " + name); }

    @GetMapping("/devices/{deviceId}")
    @Operation(summary = "Get stored telemetry records for a device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Telemetry records returned"),
            @ApiResponse(responseCode = "403", description = "Access denied: User does not own the device"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<TelemetryEvaluationResource>> getEvaluationsByDevice(
            @CurrentUserId UUID userId,
            @PathVariable UUID deviceId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!externalDeviceService.isDeviceOwnedByUser(deviceId, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Readings before the current claim were measured for a previous owner and stay hidden.
        var visibleSince = externalDeviceService.findVisibleSinceByDeviceId(deviceId).orElse(null);
        var query = new GetEvaluationsByDeviceQuery(deviceId, page, size, visibleSince);
        var evaluations = telemetryEvaluationQueryService.handle(query);
        var resources = evaluations.items().stream()
                .map(TelemetryEvaluationResourceFromEntityAssembler::toResourceFromEntity)
                .toList();

        // The port speaks PageResult; the body stays a Spring Data page so the JSON envelope
        // clients already consume is unchanged.
        return ResponseEntity.ok(new PageImpl<>(
                resources, PageRequest.of(evaluations.page(), evaluations.size()), evaluations.total()));
    }

    @GetMapping("/devices/{deviceId}/latest")
    @Operation(summary = "Get the latest telemetry record for a device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Latest record returned"),
            @ApiResponse(responseCode = "403", description = "Access denied: User does not own the device"),
            @ApiResponse(responseCode = "404", description = "No records found for device")
    })
    public ResponseEntity<TelemetryEvaluationResource> getLatestEvaluationByDevice(
            @CurrentUserId UUID userId,
            @PathVariable UUID deviceId
    ) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!externalDeviceService.isDeviceOwnedByUser(deviceId, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        var visibleSince = externalDeviceService.findVisibleSinceByDeviceId(deviceId).orElse(null);
        var query = new GetLatestEvaluationByDeviceQuery(deviceId, visibleSince);
        return telemetryEvaluationQueryService.handle(query)
                .map(e -> ResponseEntity.ok(TelemetryEvaluationResourceFromEntityAssembler.toResourceFromEntity(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    private java.util.Optional<UUID> resolveDeviceId(String deviceIdOrHardwareId) {
        try {
            UUID deviceId = UUID.fromString(deviceIdOrHardwareId);
            return externalDeviceService.findHardwareIdByDeviceId(deviceId).isPresent()
                    ? java.util.Optional.of(deviceId)
                    : java.util.Optional.empty();
        } catch (IllegalArgumentException e) {
            return externalDeviceService.findDeviceIdByHardwareId(deviceIdOrHardwareId);
        }
    }
}
