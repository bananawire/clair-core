package com.claircore.localedge.application;

import com.claircore.device.interfaces.acl.DeviceCommandForEdge;

/** Executes a claimed command without exposing device persistence to LocalEdge. */
public interface LocalDeviceCommandExecutor {
    void execute(DeviceCommandForEdge command);

    default boolean isStandby(java.util.UUID deviceId) { return false; }
}
