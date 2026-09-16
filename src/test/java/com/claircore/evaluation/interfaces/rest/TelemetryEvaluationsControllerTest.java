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
import com.claircore.iam.application.queryservices.TokenQueryService;
import com.claircore.shared.interfaces.rest.security.CurrentUserIdArgumentResolver;
import com.claircore.shared.domain.model.PageResult;
import com.claircore.shared.interfaces.rest.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TelemetryEvaluationsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TelemetryEvaluationsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TelemetryEvaluationQueryService telemetryEvaluationQueryService;

    @MockitoBean
    private TelemetryEvaluationCommandService telemetryEvaluationCommandService;

    @MockitoBean
    private ExternalDeviceService externalDeviceService;

    @MockitoBean
    private TokenQueryService tokenQueryService; // Required to satisfy context dependencies

    @Test
    void shouldRejectBatchLargerThanOperationalLimit() throws Exception {
        String record = "{\"client_ref\":\"r1\",\"device_id\":\"HW-0001\",\"reading_id\":\"00000000-0000-0000-0000-000000000123\",\"uptime_seconds\":1,\"co2\":400,\"temperature\":22,\"humidity\":45,\"pm1_0\":1,\"pm2_5\":2,\"pm10\":3,\"wifi_status\":\"ONLINE\",\"health_status\":85,\"status\":\"STABLE\",\"recorded_at\":\"2026-05-16T22:30:00Z\",\"occurred_at\":\"2026-05-16T22:30:00Z\"}";
        String body = "{\"records\":[" + String.join(",", java.util.Collections.nCopies(11, record)) + "]}";
        mockMvc.perform(post("/api/v1/evaluations/telemetry/batch").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(telemetryEvaluationCommandService);
    }

    @Test
    void shouldRejectBatchWithMissingOrInvalidOccurredAt() throws Exception {
        String missing = "{\"records\":[{\"client_ref\":\"r1\",\"device_id\":\"HW-0001\",\"reading_id\":\"00000000-0000-0000-0000-000000000123\",\"uptime_seconds\":1,\"co2\":400,\"temperature\":22,\"humidity\":45,\"pm1_0\":1,\"pm2_5\":2,\"pm10\":3,\"wifi_status\":\"ONLINE\",\"health_status\":85,\"status\":\"STABLE\",\"recorded_at\":\"2026-05-16T22:30:00Z\"}]}";
        String invalid = missing.replace("\"recorded_at\":\"2026-05-16T22:30:00Z\"}", "\"recorded_at\":\"2026-05-16T22:30:00Z\",\"occurred_at\":\"not-a-timestamp\"}");
        mockMvc.perform(post("/api/v1/evaluations/telemetry/batch").contentType(MediaType.APPLICATION_JSON).content(missing))
                .andExpect(status().isOk()).andExpect(jsonPath("$.results[0].reason").value("VALIDATION_ERROR"));
        mockMvc.perform(post("/api/v1/evaluations/telemetry/batch").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isOk()).andExpect(jsonPath("$.results[0].reason").value("VALIDATION_ERROR"));
        verifyNoInteractions(telemetryEvaluationCommandService);
    }

    @Test
    void shouldAcceptValidBatchAndCorrelateEachClientReference() throws Exception {
        UUID firstDevice = UUID.randomUUID();
        UUID secondDevice = UUID.randomUUID();
        when(externalDeviceService.findHardwareIdByDeviceId(firstDevice)).thenReturn(Optional.of("HW-0001"));
        when(externalDeviceService.findHardwareIdByDeviceId(secondDevice)).thenReturn(Optional.of("HW-0002"));
        TelemetryEvaluation evaluation = new TelemetryEvaluation(
                new DeviceId(firstDevice), UUID.fromString("00000000-0000-0000-0000-000000000123"), 3600L,
                new AirQuality(400.0, 22.0, 45.0),
                new ParticulateMatter(1.0, 2.0, 3.0),
                new Connectivity("ONLINE", "WiFi", -50),
                new Location("Chile"), 85, "STABLE", Instant.parse("2026-05-16T22:30:00Z"));
        when(telemetryEvaluationCommandService.handle(any(EvaluateTelemetryCommand.class))).thenReturn(evaluation);

        String record = "{\"client_ref\":\"%s\",\"device_id\":\"%s\",\"reading_id\":\"00000000-0000-0000-0000-000000000123\",\"uptime_seconds\":3600,\"co2\":400,\"temperature\":22,\"humidity\":45,\"pm1_0\":1,\"pm2_5\":12.45,\"pm10\":3,\"wifi_status\":\"ONLINE\",\"network_name\":\"WiFi\",\"signal_strength\":-50,\"country\":\"Chile\",\"health_status\":85,\"status\":\"STABLE\",\"recorded_at\":\"2026-05-16T22:30:00Z\",\"occurred_at\":\"2026-05-16T17:29:00-05:00\"}";
        String body = "{\"records\":[" + record.formatted("outbox-1", firstDevice) + "," + record.formatted("outbox-2", secondDevice) + "]}";

        mockMvc.perform(post("/api/v1/evaluations/telemetry/batch")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].client_ref").value("outbox-1"))
                .andExpect(jsonPath("$.results[0].status").value("CREATED"))
                .andExpect(jsonPath("$.results[1].client_ref").value("outbox-2"))
                .andExpect(jsonPath("$.results[1].status").value("CREATED"));
        var captured = org.mockito.ArgumentCaptor.forClass(EvaluateTelemetryCommand.class);
        verify(telemetryEvaluationCommandService, times(2)).handle(captured.capture());
        org.assertj.core.api.Assertions.assertThat(captured.getAllValues()).allSatisfy(command -> {
            org.assertj.core.api.Assertions.assertThat(command.recordedAt()).isEqualTo(Instant.parse("2026-05-16T22:29:00Z"));
            org.assertj.core.api.Assertions.assertThat(command.particulateMatter().pm2_5()).isEqualTo(12.45);
            org.assertj.core.api.Assertions.assertThat(command.readingId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000123"));
        });
    }

    @Test
    void shouldReturnValidationErrorForNonNumericBatchValue() throws Exception {
        String body = "{\"records\":[{\"client_ref\":\"r1\",\"device_id\":\"HW-0001\",\"reading_id\":\"00000000-0000-0000-0000-000000000123\",\"uptime_seconds\":\"abc\",\"co2\":400,\"temperature\":22,\"humidity\":45,\"pm1_0\":1,\"pm2_5\":2,\"pm10\":3,\"wifi_status\":\"ONLINE\",\"health_status\":85,\"status\":\"STABLE\",\"recorded_at\":\"2026-05-16T22:30:00Z\"}]}";
        mockMvc.perform(post("/api/v1/evaluations/telemetry/batch").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.results[0].status").value("ERROR"))
                .andExpect(jsonPath("$.results[0].reason").value("VALIDATION_ERROR"));
        verifyNoInteractions(telemetryEvaluationCommandService);
    }

    @Test
    void shouldReturnCreatedWhenEvaluatingValidTelemetryWithUuidDevice() throws Exception {
        // Arrange
        UUID resolvedDeviceId = UUID.randomUUID();
        when(externalDeviceService.findHardwareIdByDeviceId(resolvedDeviceId)).thenReturn(Optional.of("HW-001"));

        TelemetryEvaluation evaluation = new TelemetryEvaluation(
                new DeviceId(resolvedDeviceId), UUID.fromString("00000000-0000-0000-0000-000000000123"), 3600L,
                new AirQuality(400.0, 22.0, 45.0),
                new ParticulateMatter(10.0, 15.0, 25.0),
                new Connectivity("ONLINE", "WiFi", -50),
                new Location("Chile"),
                85, "STABLE", Instant.now()
        );
        when(telemetryEvaluationCommandService.handle(any(EvaluateTelemetryCommand.class))).thenReturn(evaluation);

        var requestBody = new EvaluateTelemetryResource(
                resolvedDeviceId.toString(),
                "00000000-0000-0000-0000-000000000123",
                "3600",
                new EvaluateTelemetryResource.AirQualityResource(400.0, 22.0, 45.0),
                new EvaluateTelemetryResource.ParticulateMatterResource(10.0, 15.0, 25.0),
                new EvaluateTelemetryResource.ConnectivityResource("ONLINE", "WiFi", -50),
                new EvaluateTelemetryResource.LocationResource("Chile"),
                85,
                "STABLE",
                Instant.now().toString()
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/evaluations/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deviceId").value(resolvedDeviceId.toString()));

        // Verification: Since a UUID was passed, findDeviceIdByHardwareId should NOT be called
        verify(externalDeviceService, never()).findDeviceIdByHardwareId(anyString());
        verify(externalDeviceService).findHardwareIdByDeviceId(resolvedDeviceId);
    }

    @Test
    void shouldReturnCreatedWhenEvaluatingValidTelemetryWithHardwareIdDevice() throws Exception {
        // Arrange
        String hardwareId = "CLAIR-001";
        UUID resolvedDeviceId = UUID.randomUUID();
        when(externalDeviceService.findDeviceIdByHardwareId(hardwareId)).thenReturn(Optional.of(resolvedDeviceId));

        TelemetryEvaluation evaluation = new TelemetryEvaluation(
                new DeviceId(resolvedDeviceId), UUID.fromString("00000000-0000-0000-0000-000000000123"), 3600L,
                new AirQuality(400.0, 22.0, 45.0),
                new ParticulateMatter(10.0, 15.0, 25.0),
                new Connectivity("ONLINE", "WiFi", -50),
                new Location("Chile"),
                85, "STABLE", Instant.now()
        );
        when(telemetryEvaluationCommandService.handle(any(EvaluateTelemetryCommand.class))).thenReturn(evaluation);

        var requestBody = new EvaluateTelemetryResource(
                hardwareId,
                "00000000-0000-0000-0000-000000000123",
                "3600",
                new EvaluateTelemetryResource.AirQualityResource(400.0, 22.0, 45.0),
                new EvaluateTelemetryResource.ParticulateMatterResource(10.0, 15.0, 25.0),
                new EvaluateTelemetryResource.ConnectivityResource("ONLINE", "WiFi", -50),
                new EvaluateTelemetryResource.LocationResource("Chile"),
                85,
                "STABLE",
                Instant.now().toString()
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/evaluations/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deviceId").value(resolvedDeviceId.toString()));

        // Verification: Verify findDeviceIdByHardwareId is explicitly called
        verify(externalDeviceService, times(1)).findDeviceIdByHardwareId(hardwareId);
    }

    @Test
    void shouldReturnBadRequestWhenTimestampIsInvalid() throws Exception {
        // Arrange
        UUID resolvedDeviceId = UUID.randomUUID();
        when(externalDeviceService.findHardwareIdByDeviceId(resolvedDeviceId)).thenReturn(Optional.of("HW-002"));

        var requestBody = new EvaluateTelemetryResource(
                resolvedDeviceId.toString(),
                "invalid-time-format",
                "3600",
                new EvaluateTelemetryResource.AirQualityResource(400.0, 22.0, 45.0),
                new EvaluateTelemetryResource.ParticulateMatterResource(10.0, 15.0, 25.0),
                new EvaluateTelemetryResource.ConnectivityResource("ONLINE", "WiFi", -50),
                new EvaluateTelemetryResource.LocationResource("Chile"),
                85,
                "STABLE",
                null
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/evaluations/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenUptimeIsInvalid() throws Exception {
        // Arrange
        UUID resolvedDeviceId = UUID.randomUUID();
        when(externalDeviceService.findHardwareIdByDeviceId(resolvedDeviceId)).thenReturn(Optional.of("HW-003"));

        var requestBody = new EvaluateTelemetryResource(
                resolvedDeviceId.toString(),
                "00000000-0000-0000-0000-000000000123",
                "invalid-uptime-format",
                new EvaluateTelemetryResource.AirQualityResource(400.0, 22.0, 45.0),
                new EvaluateTelemetryResource.ParticulateMatterResource(10.0, 15.0, 25.0),
                new EvaluateTelemetryResource.ConnectivityResource("ONLINE", "WiFi", -50),
                new EvaluateTelemetryResource.LocationResource("Chile"),
                85,
                "STABLE",
                null
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/evaluations/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenCreatedAtIsInvalid() throws Exception {
        // Arrange
        String hardwareId = "CLAIR-002";
        UUID resolvedDeviceId = UUID.randomUUID();
        when(externalDeviceService.findDeviceIdByHardwareId(hardwareId)).thenReturn(Optional.of(resolvedDeviceId));

        var requestBody = new EvaluateTelemetryResource(
                hardwareId,
                "00000000-0000-0000-0000-000000000123",
                "3600",
                new EvaluateTelemetryResource.AirQualityResource(400.0, 22.0, 45.0),
                new EvaluateTelemetryResource.ParticulateMatterResource(10.0, 15.0, 25.0),
                new EvaluateTelemetryResource.ConnectivityResource("ONLINE", "WiFi", -50),
                new EvaluateTelemetryResource.LocationResource("Chile"),
                85,
                "STABLE",
                "invalid-instant"
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/evaluations/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFoundWhenEvaluatingTelemetryForUnknownDevice() throws Exception {
        // Arrange
        String unknownDevice = "unknown-device-id";
        when(externalDeviceService.findDeviceIdByHardwareId(unknownDevice)).thenReturn(Optional.empty());

        var requestBody = new EvaluateTelemetryResource(
                unknownDevice,
                "00000000-0000-0000-0000-000000000123",
                "3600",
                new EvaluateTelemetryResource.AirQualityResource(400.0, 22.0, 45.0),
                new EvaluateTelemetryResource.ParticulateMatterResource(10.0, 15.0, 25.0),
                new EvaluateTelemetryResource.ConnectivityResource("ONLINE", "WiFi", -50),
                new EvaluateTelemetryResource.LocationResource("Chile"),
                85,
                "STABLE",
                "2026-05-16T22:30:00Z"
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/evaluations/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnNotFoundWhenEvaluatingTelemetryForUnknownUuidDevice() throws Exception {
        // Arrange
        UUID unknownDevice = UUID.randomUUID();
        when(externalDeviceService.findHardwareIdByDeviceId(unknownDevice)).thenReturn(Optional.empty());

        var requestBody = new EvaluateTelemetryResource(
                unknownDevice.toString(),
                "00000000-0000-0000-0000-000000000123",
                "3600",
                new EvaluateTelemetryResource.AirQualityResource(400.0, 22.0, 45.0),
                new EvaluateTelemetryResource.ParticulateMatterResource(10.0, 15.0, 25.0),
                new EvaluateTelemetryResource.ConnectivityResource("ONLINE", "WiFi", -50),
                new EvaluateTelemetryResource.LocationResource("Chile"),
                85,
                "STABLE",
                "2026-05-16T22:30:00Z"
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/evaluations/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnUnauthorizedWhenUserIdIsMissingFromRequestAttributes() throws Exception {
        // Arrange
        UUID deviceId = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(get("/api/v1/evaluations/devices/{deviceId}", deviceId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnForbiddenWhenUserDoesNotOwnDevice() throws Exception {
        // Arrange
        UUID deviceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(externalDeviceService.isDeviceOwnedByUser(deviceId, userId)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/v1/evaluations/devices/{deviceId}", deviceId)
                        .requestAttr(CurrentUserIdArgumentResolver.USER_ID_ATTRIBUTE, userId))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnEvaluationsWhenUserOwnsDevice() throws Exception {
        // Arrange
        UUID deviceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(externalDeviceService.isDeviceOwnedByUser(deviceId, userId)).thenReturn(true);

        TelemetryEvaluation evaluation = new TelemetryEvaluation(
                new DeviceId(deviceId), UUID.fromString("00000000-0000-0000-0000-000000000123"), 3600L,
                new AirQuality(400.0, 22.0, 45.0),
                new ParticulateMatter(10.0, 15.0, 25.0),
                new Connectivity("ONLINE", "WiFi", -50),
                new Location("Chile"),
                85, "STABLE", Instant.now()
        );
        var page = new PageResult<>(List.of(evaluation), 0, 20, 1L);
        when(telemetryEvaluationQueryService.handle(any(GetEvaluationsByDeviceQuery.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/v1/evaluations/devices/{deviceId}", deviceId)
                        .requestAttr(CurrentUserIdArgumentResolver.USER_ID_ATTRIBUTE, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].deviceId").value(deviceId.toString()));
    }

    @Test
    void shouldReturnLatestEvaluationWhenUserOwnsDevice() throws Exception {
        // Arrange
        UUID deviceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(externalDeviceService.isDeviceOwnedByUser(deviceId, userId)).thenReturn(true);

        TelemetryEvaluation evaluation = new TelemetryEvaluation(
                new DeviceId(deviceId), UUID.fromString("00000000-0000-0000-0000-000000000123"), 3600L,
                new AirQuality(400.0, 22.0, 45.0),
                new ParticulateMatter(10.0, 15.0, 25.0),
                new Connectivity("ONLINE", "WiFi", -50),
                new Location("Chile"),
                85, "STABLE", Instant.now()
        );
        when(telemetryEvaluationQueryService.handle(any(GetLatestEvaluationByDeviceQuery.class)))
                .thenReturn(Optional.of(evaluation));

        // Act & Assert
        mockMvc.perform(get("/api/v1/evaluations/devices/{deviceId}/latest", deviceId)
                        .requestAttr(CurrentUserIdArgumentResolver.USER_ID_ATTRIBUTE, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value(deviceId.toString()));
    }

    @Test
    void shouldReturnNotFoundWhenNoLatestEvaluationExists() throws Exception {
        // Arrange
        UUID deviceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(externalDeviceService.isDeviceOwnedByUser(deviceId, userId)).thenReturn(true);
        when(telemetryEvaluationQueryService.handle(any(GetLatestEvaluationByDeviceQuery.class)))
                .thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/v1/evaluations/devices/{deviceId}/latest", deviceId)
                        .requestAttr(CurrentUserIdArgumentResolver.USER_ID_ATTRIBUTE, userId))
                .andExpect(status().isNotFound());
    }
    @Test
    void ownerReadsAreClampedToTheCurrentClaim() throws Exception {
        UUID deviceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        java.time.Instant claimedAt = java.time.Instant.parse("2026-09-10T12:00:00Z");
        when(externalDeviceService.isDeviceOwnedByUser(deviceId, userId)).thenReturn(true);
        when(externalDeviceService.findVisibleSinceByDeviceId(deviceId)).thenReturn(java.util.Optional.of(claimedAt));
        when(telemetryEvaluationQueryService.handle(org.mockito.ArgumentMatchers.any(GetEvaluationsByDeviceQuery.class)))
                .thenReturn(com.claircore.shared.domain.model.PageResult.empty(0, 20));
        when(telemetryEvaluationQueryService.handle(org.mockito.ArgumentMatchers.any(GetLatestEvaluationByDeviceQuery.class)))
                .thenReturn(java.util.Optional.empty());
        mockMvc.perform(get("/api/v1/evaluations/devices/{deviceId}", deviceId)
                        .requestAttr(CurrentUserIdArgumentResolver.USER_ID_ATTRIBUTE, userId))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/evaluations/devices/{deviceId}/latest", deviceId)
                        .requestAttr(CurrentUserIdArgumentResolver.USER_ID_ATTRIBUTE, userId))
                .andExpect(status().isNotFound());
        var pageCaptor = org.mockito.ArgumentCaptor.forClass(GetEvaluationsByDeviceQuery.class);
        org.mockito.Mockito.verify(telemetryEvaluationQueryService).handle(pageCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(pageCaptor.getValue().visibleSince()).isEqualTo(claimedAt);
        var latestCaptor = org.mockito.ArgumentCaptor.forClass(GetLatestEvaluationByDeviceQuery.class);
        org.mockito.Mockito.verify(telemetryEvaluationQueryService).handle(latestCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(latestCaptor.getValue().visibleSince()).isEqualTo(claimedAt);
    }
}
