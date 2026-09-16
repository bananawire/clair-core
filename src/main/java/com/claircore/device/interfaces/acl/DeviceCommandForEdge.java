package com.claircore.device.interfaces.acl;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Stable command representation exposed to LocalEdge; persistence and domain types never cross this ACL. */
public record DeviceCommandForEdge(
        UUID commandId,
        UUID deviceId,
        UUID assignmentId,
        String type,
        String payload,
        Instant sentAt) {

    private static final Set<String> SUPPORTED_TYPES = Set.of("STANDBY", "WAKE", "RESTART");

    public DeviceCommandForEdge {
        if (commandId == null) {
            throw new IllegalArgumentException("commandId must not be null");
        }
        if (deviceId == null) {
            throw new IllegalArgumentException("deviceId must not be null");
        }
        if (type == null || !SUPPORTED_TYPES.contains(type)) {
            throw new IllegalArgumentException("Unknown device command type: " + type);
        }
    }

    /** Whether this command suspends the simulated device telemetry. */
    public boolean isStandby() {
        return "STANDBY".equals(type);
    }
}
