package com.claircore.localedge.application.internal.commandservices;

import com.claircore.device.interfaces.acl.DeviceTelemetryTarget;
import com.claircore.evaluation.interfaces.acl.TelemetryRecordingResult;
import com.claircore.evaluation.interfaces.acl.TelemetrySubmission;
import com.claircore.localedge.application.internal.outboundservices.acl.ExternalDeviceService;
import com.claircore.localedge.application.internal.outboundservices.acl.ExternalEvaluationService;
import com.claircore.localedge.domain.model.commands.GenerateSyntheticTelemetryCommand;
import com.claircore.localedge.domain.model.valueobjects.SimulationScenario;
import com.claircore.localedge.domain.services.SyntheticTelemetryGeneratorPolicy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalEdgeTelemetryCommandServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-05-16T22:30:00Z");

    @Mock ExternalDeviceService externalDeviceService;
    @Mock ExternalEvaluationService externalEvaluationService;
    private SyntheticTelemetryGeneratorPolicy policy;
    private LocalEdgeTelemetryCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        policy = new SyntheticTelemetryGeneratorPolicy(42L);
        service = new LocalEdgeTelemetryCommandServiceImpl(externalDeviceService, externalEvaluationService, policy);
    }

    @Test
    void oneCycleProducesOneReadingAndOnePresenceUpdatePerTarget() {
        UUID device = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(externalDeviceService.findTelemetryTargets(eq(1), anyBoolean()))
                .thenReturn(List.of(new DeviceTelemetryTarget(device, "HW-0001", "Sensor 1", true)));
        when(externalEvaluationService.recordTelemetry(any()))
                .thenReturn(TelemetryRecordingResult.accepted(UUID.randomUUID(), NOW));

        var command = new GenerateSyntheticTelemetryCommand(List.of(device), SimulationScenario.MIXED, 42L, NOW);
        LocalEdgeTelemetryCommandService.CycleOutcome outcome = service.runCycle(command);

        assertThat(outcome.accepted()).isEqualTo(1);
        assertThat(outcome.rejected()).isZero();
        assertThat(outcome.presenceUpdates()).isEqualTo(1);

        ArgumentCaptor<TelemetrySubmission> submission = ArgumentCaptor.forClass(TelemetrySubmission.class);
        verify(externalEvaluationService).recordTelemetry(submission.capture());
        assertThat(submission.getValue().deviceId()).isEqualTo(device);
        assertThat(submission.getValue().country()).isEqualTo("Peru");
        assertThat(submission.getValue().networkName()).isEqualTo("LOCAL-SIMULATOR");
        verify(externalDeviceService).recordPresence(eq(device), eq("ONLINE"), eq(NOW));
    }

    @Test
    void emptyTargetListSkipsTheCycle() {
        when(externalDeviceService.findTelemetryTargets(anyInt(), anyBoolean())).thenReturn(List.of());

        var command = new GenerateSyntheticTelemetryCommand(
                List.of(UUID.randomUUID()), SimulationScenario.MIXED, 1L, NOW);
        LocalEdgeTelemetryCommandService.CycleOutcome outcome = service.runCycle(command);

        assertThat(outcome.accepted()).isZero();
        assertThat(outcome.rejected()).isZero();
        assertThat(outcome.presenceUpdates()).isZero();
        verify(externalEvaluationService, never()).recordTelemetry(any());
    }

    @Test
    void rejectedReadingIsCountedButDoesNotThrow() {
        UUID device = UUID.randomUUID();
        when(externalDeviceService.findTelemetryTargets(eq(1), anyBoolean()))
                .thenReturn(List.of(new DeviceTelemetryTarget(device, "HW-0001", "Sensor 1", true)));
        when(externalEvaluationService.recordTelemetry(any()))
                .thenReturn(TelemetryRecordingResult.rejected());

        var command = new GenerateSyntheticTelemetryCommand(List.of(device), SimulationScenario.MIXED, 1L, NOW);
        LocalEdgeTelemetryCommandService.CycleOutcome outcome = service.runCycle(command);

        assertThat(outcome.accepted()).isZero();
        assertThat(outcome.rejected()).isEqualTo(1);
        verify(externalDeviceService, times(1)).recordPresence(eq(device), eq("ONLINE"), eq(NOW));
    }

    @Test
    void unknownStatusFromEvaluationIsSwallowedAsReject() {
        UUID device = UUID.randomUUID();
        when(externalDeviceService.findTelemetryTargets(eq(1), anyBoolean()))
                .thenReturn(List.of(new DeviceTelemetryTarget(device, "HW-0001", "Sensor 1", true)));
        when(externalEvaluationService.recordTelemetry(any()))
                .thenThrow(new IllegalStateException("downstream unavailable"));

        var command = new GenerateSyntheticTelemetryCommand(List.of(device), SimulationScenario.MIXED, 1L, NOW);
        LocalEdgeTelemetryCommandService.CycleOutcome outcome = service.runCycle(command);

        assertThat(outcome.accepted()).isZero();
        assertThat(outcome.rejected()).isEqualTo(1);
        // Presence update is independent of the reading dispatch; it runs even when the
        // evaluation BC is unhealthy so the device's assignment timestamp stays current.
        verify(externalDeviceService).recordPresence(eq(device), eq("ONLINE"), eq(NOW));
    }
}
