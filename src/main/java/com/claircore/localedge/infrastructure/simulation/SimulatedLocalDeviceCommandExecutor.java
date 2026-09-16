package com.claircore.localedge.infrastructure.simulation;

import com.claircore.device.interfaces.acl.DeviceCommandForEdge;
import com.claircore.localedge.application.LocalDeviceCommandExecutor;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** In-process actuator simulation. STANDBY suppresses telemetry; WAKE/RESTART resumes it. */
public class SimulatedLocalDeviceCommandExecutor implements LocalDeviceCommandExecutor {
    private final Set<UUID> standbyDevices = ConcurrentHashMap.newKeySet();

    @Override
    public void execute(DeviceCommandForEdge command) {
        if (command == null) {
            throw new IllegalArgumentException("command must not be null");
        }
        if (command.isStandby()) {
            standbyDevices.add(command.deviceId());
        } else {
            // The ACL contract admits only WAKE and RESTART as the non-standby operations.
            standbyDevices.remove(command.deviceId());
        }
    }

    @Override
    public boolean isStandby(UUID deviceId) {
        return deviceId != null && standbyDevices.contains(deviceId);
    }
}
