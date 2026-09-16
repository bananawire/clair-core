package com.claircore.localedge.infrastructure.simulation;

import com.claircore.device.interfaces.acl.DeviceCommandForEdge;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatedLocalDeviceCommandExecutorTest {
    @Test
    void shouldSuspendAndResumeTelemetryForPowerCommands() {
        var executor = new SimulatedLocalDeviceCommandExecutor();
        UUID device = UUID.randomUUID();
        var standby = new DeviceCommandForEdge(UUID.randomUUID(), device, null,
                "STANDBY", null, null);
        var wake = new DeviceCommandForEdge(UUID.randomUUID(), device, null,
                "WAKE", null, null);
        var restart = new DeviceCommandForEdge(UUID.randomUUID(), device, null,
                "RESTART", null, null);

        executor.execute(standby);
        assertThat(executor.isStandby(device)).isTrue();
        executor.execute(wake);
        assertThat(executor.isStandby(device)).isFalse();
        executor.execute(standby);
        executor.execute(restart);
        assertThat(executor.isStandby(device)).isFalse();
    }
}
