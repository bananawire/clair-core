package com.claircore.localedge.application.internal.schedulers;

import com.claircore.device.interfaces.acl.DeviceTelemetryTarget;
import com.claircore.localedge.application.internal.commandservices.LocalEdgeTelemetryCommandService;
import com.claircore.localedge.application.internal.outboundservices.acl.ExternalDeviceService;
import com.claircore.localedge.domain.model.commands.GenerateSyntheticTelemetryCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalEdgeTelemetrySchedulerTest {

    private static final UUID ACTIVE_DEVICE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID STANDBY_DEVICE = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private ExternalDeviceService devices;
    @Mock
    private LocalEdgeTelemetryCommandService commandService;

    @Test
    void shouldScheduleOnlyActiveTargets() {
        when(devices.findTelemetryTargets(10, false)).thenReturn(List.of(
                new DeviceTelemetryTarget(STANDBY_DEVICE, "HW-0001", "Standby", true, "STANDBY"),
                new DeviceTelemetryTarget(ACTIVE_DEVICE, "HW-0002", "Active", true, "ONLINE")));
        when(commandService.handle(any(GenerateSyntheticTelemetryCommand.class))).thenReturn(1);

        new LocalEdgeTelemetryScheduler(devices, commandService, "MIXED", 7L, 10).cycle();

        ArgumentCaptor<GenerateSyntheticTelemetryCommand> captor =
                ArgumentCaptor.forClass(GenerateSyntheticTelemetryCommand.class);
        verify(commandService).handle(captor.capture());
        assertThat(captor.getValue().deviceIds()).containsExactly(ACTIVE_DEVICE);
    }

    @Test
    void shouldSkipTheCycleWhenAllTargetsAreStandby() {
        when(devices.findTelemetryTargets(10, false)).thenReturn(List.of(
                new DeviceTelemetryTarget(STANDBY_DEVICE, "HW-0001", "Standby", true, "STANDBY")));

        new LocalEdgeTelemetryScheduler(devices, commandService, "MIXED", 7L, 10).cycle();

        verify(commandService, never()).handle(any(GenerateSyntheticTelemetryCommand.class));
    }

    @Test
    void shouldSkipTheCycleWhenTargetLimitIsDisabled() {
        new LocalEdgeTelemetryScheduler(devices, commandService, "MIXED", 7L, 0).cycle();

        verify(devices, never()).findTelemetryTargets(anyInt(), anyBoolean());
        verify(commandService, never()).handle(any(GenerateSyntheticTelemetryCommand.class));
    }
}
