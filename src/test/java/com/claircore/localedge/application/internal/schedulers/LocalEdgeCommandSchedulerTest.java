package com.claircore.localedge.application.internal.schedulers;

import com.claircore.device.interfaces.acl.DeviceCommandForEdge;
import com.claircore.localedge.application.LocalDeviceCommandExecutor;
import com.claircore.localedge.application.internal.outboundservices.acl.ExternalDeviceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalEdgeCommandSchedulerTest {

    @Mock
    private ExternalDeviceService devices;
    @Mock
    private LocalDeviceCommandExecutor executor;

    @Test
    void shouldClaimExecuteAndAcknowledgeEachCommand() {
        DeviceCommandForEdge command = command("STANDBY");
        when(devices.findClaimableCommands(any(), eq(25))).thenReturn(List.of(command));
        when(devices.claimCommand(eq(command.commandId()), any(), any()))
                .thenReturn(Optional.of(command));

        new LocalEdgeCommandScheduler(devices, executor, 25, 60).poll();

        verify(executor).execute(command);
        verify(devices).acknowledgeCommand(command, true, null);
    }

    @Test
    void shouldAcknowledgeFailureWhenExecutionThrows() {
        DeviceCommandForEdge command = command("RESTART");
        when(devices.findClaimableCommands(any(), eq(25))).thenReturn(List.of(command));
        when(devices.claimCommand(eq(command.commandId()), any(), any()))
                .thenReturn(Optional.of(command));
        doThrow(new IllegalStateException("simulated actuator failure"))
                .when(executor).execute(command);

        new LocalEdgeCommandScheduler(devices, executor, 25, 60).poll();

        verify(devices).acknowledgeCommand(command, false, "simulated actuator failure");
    }

    @Test
    void shouldLeaveAClaimForLeaseRecoveryWhenAckFails() {
        DeviceCommandForEdge command = command("WAKE");
        when(devices.findClaimableCommands(any(), eq(25))).thenReturn(List.of(command));
        when(devices.claimCommand(eq(command.commandId()), any(), any()))
                .thenReturn(Optional.of(command));
        doThrow(new IllegalStateException("acknowledgement unavailable"))
                .when(devices).acknowledgeCommand(command, true, null);

        new LocalEdgeCommandScheduler(devices, executor, 25, 60).poll();

        verify(devices).acknowledgeCommand(command, true, null);
        verify(devices, never()).acknowledgeCommand(eq(command), eq(false), any());
    }

    @Test
    void shouldNotExecuteWhenAnotherConsumerWinsTheClaim() {
        DeviceCommandForEdge command = command("WAKE");
        when(devices.findClaimableCommands(any(), eq(25))).thenReturn(List.of(command));
        when(devices.claimCommand(eq(command.commandId()), any(), any()))
                .thenReturn(Optional.empty());

        new LocalEdgeCommandScheduler(devices, executor, 25, 60).poll();

        verify(executor, never()).execute(any());
        verify(devices, never()).acknowledgeCommand(any(), any(Boolean.class), any());
    }

    private static DeviceCommandForEdge command(String type) {
        return new DeviceCommandForEdge(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), type, null, null);
    }
}
