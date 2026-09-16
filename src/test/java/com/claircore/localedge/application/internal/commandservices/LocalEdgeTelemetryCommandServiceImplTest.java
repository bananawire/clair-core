package com.claircore.localedge.application.internal.commandservices;

import com.claircore.device.interfaces.acl.DeviceTelemetryTarget;
import com.claircore.evaluation.interfaces.acl.TelemetryRecordingResult;
import com.claircore.evaluation.interfaces.acl.TelemetrySubmission;
import com.claircore.device.interfaces.acl.DeviceCommandForEdge;
import com.claircore.localedge.application.LocalDeviceCommandExecutor;
import com.claircore.localedge.application.internal.outboundservices.acl.ExternalDeviceService;
import com.claircore.localedge.infrastructure.simulation.SimulatedLocalDeviceCommandExecutor;
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
    @Mock LocalDeviceCommandExecutor commandExecutor;
    private SyntheticTelemetryGeneratorPolicy policy;
    private LocalEdgeTelemetryCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        policy = new SyntheticTelemetryGeneratorPolicy(42L);
        service = new LocalEdgeTelemetryCommandServiceImpl(
                externalDeviceService, externalEvaluationService, policy, commandExecutor);
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
    void unassignedTargetReceivesTelemetryButNotPresenceUpdate() {
        UUID device = UUID.randomUUID();
        when(externalDeviceService.findTelemetryTargets(eq(1), anyBoolean()))
                .thenReturn(List.of(new DeviceTelemetryTarget(device, "HW-0001", "Sensor 1", false)));
        when(externalEvaluationService.recordTelemetry(any()))
                .thenReturn(TelemetryRecordingResult.accepted(UUID.randomUUID(), NOW));

        var command = new GenerateSyntheticTelemetryCommand(List.of(device), SimulationScenario.MIXED, 1L, NOW);
        LocalEdgeTelemetryCommandServiceImpl.CycleOutcome outcome = service.runCycle(command);

        assertThat(outcome.accepted()).isEqualTo(1);
        assertThat(outcome.rejected()).isZero();
        assertThat(outcome.presenceUpdates()).isZero();
        verify(externalDeviceService, never()).recordPresence(any(), anyString(), any());
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
    void standbyTargetDoesNotProduceTelemetryOrPresence() {
        UUID device = UUID.randomUUID();
        when(externalDeviceService.findTelemetryTargets(eq(1), anyBoolean()))
                .thenReturn(List.of(new DeviceTelemetryTarget(device, "HW-0001", "Sensor 1", true, "STANDBY")));

        var command = new GenerateSyntheticTelemetryCommand(List.of(device), SimulationScenario.MIXED, 1L, NOW);
        LocalEdgeTelemetryCommandServiceImpl.CycleOutcome outcome = service.runCycle(command);

        assertThat(outcome.accepted()).isZero();
        assertThat(outcome.rejected()).isZero();
        assertThat(outcome.presenceUpdates()).isZero();
        verify(externalEvaluationService, never()).recordTelemetry(any());
        verify(externalDeviceService, never()).recordPresence(any(), anyString(), any());
    }

    @Test
    void executorStandbyStateSuppressesTelemetryBeforeAclStatusCatchesUp() {
        UUID device = UUID.randomUUID();
        when(externalDeviceService.findTelemetryTargets(eq(1), anyBoolean()))
                .thenReturn(List.of(new DeviceTelemetryTarget(device, "HW-0001", "Sensor 1", true, "ONLINE")));
        when(commandExecutor.isStandby(device)).thenReturn(true);

        var command = new GenerateSyntheticTelemetryCommand(List.of(device), SimulationScenario.MIXED, 1L, NOW);
        LocalEdgeTelemetryCommandServiceImpl.CycleOutcome outcome = service.runCycle(command);

        assertThat(outcome.accepted()).isZero();
        verify(externalEvaluationService, never()).recordTelemetry(any());
        verify(externalDeviceService, never()).recordPresence(any(), anyString(), any());
    }

    @Test
    void wakeResumesTelemetryAfterStandbySuppressedIt() {
        UUID device = UUID.randomUUID();
        when(externalDeviceService.findTelemetryTargets(eq(1), anyBoolean()))
                .thenReturn(List.of(new DeviceTelemetryTarget(device, "HW-0001", "Sensor 1", true, "ONLINE")));
        when(externalEvaluationService.recordTelemetry(any()))
                .thenReturn(TelemetryRecordingResult.accepted(UUID.randomUUID(), NOW));

        SimulatedLocalDeviceCommandExecutor executor = new SimulatedLocalDeviceCommandExecutor();
        LocalEdgeTelemetryCommandServiceImpl statefulService = new LocalEdgeTelemetryCommandServiceImpl(
                externalDeviceService, externalEvaluationService,
                new SyntheticTelemetryGeneratorPolicy(42L), executor);
        var standby = new DeviceCommandForEdge(UUID.randomUUID(), device, UUID.randomUUID(),
                "STANDBY", null, NOW);
        var wake = new DeviceCommandForEdge(UUID.randomUUID(), device, UUID.randomUUID(),
                "WAKE", null, NOW);

        executor.execute(standby);
        var standbyOutcome = statefulService.runCycle(new GenerateSyntheticTelemetryCommand(
                List.of(device), SimulationScenario.MIXED, 42L, NOW));
        assertThat(standbyOutcome.accepted()).isZero();
        verify(externalEvaluationService, never()).recordTelemetry(any());

        executor.execute(wake);
        var wakeOutcome = statefulService.runCycle(new GenerateSyntheticTelemetryCommand(
                List.of(device), SimulationScenario.MIXED, 42L, NOW.plusSeconds(15)));
        assertThat(wakeOutcome.accepted()).isEqualTo(1);
        assertThat(wakeOutcome.presenceUpdates()).isEqualTo(1);
    }

    @Test
    void commandDeviceNotReturnedByAclDoesNotReceiveTelemetry() {
        UUID requested = UUID.randomUUID();
        UUID surfaced = UUID.randomUUID();
        when(externalDeviceService.findTelemetryTargets(eq(1), anyBoolean()))
                .thenReturn(List.of(new DeviceTelemetryTarget(surfaced, "HW-0001", "Sensor 1", true)));

        var command = new GenerateSyntheticTelemetryCommand(
                List.of(requested), SimulationScenario.MIXED, 1L, NOW);
        LocalEdgeTelemetryCommandServiceImpl.CycleOutcome outcome = service.runCycle(command);

        assertThat(outcome.accepted()).isZero();
        assertThat(outcome.rejected()).isZero();
        verify(externalEvaluationService, never()).recordTelemetry(any());
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
