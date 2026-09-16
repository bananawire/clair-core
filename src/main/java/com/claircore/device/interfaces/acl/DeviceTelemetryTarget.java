package com.claircore.device.interfaces.acl;

import java.util.UUID;

/**
 * Picked-up-by-the-edge view of a device: the LocalEdge schedules against this shape, never
 * against the full Device aggregate. {@code assigned} is true if a {@code DeviceAssignment} row
 * exists; inventory-only devices can still produce telemetry, but have no presence row to update.
 * The status is a published label rather than the device bounded context's domain enum.
 */
public record DeviceTelemetryTarget(
        UUID deviceId,
        String hardwareId,
        String name,
        boolean assigned,
        String status
) {
    public DeviceTelemetryTarget(UUID deviceId, String hardwareId, String name, boolean assigned) {
        this(deviceId, hardwareId, name, assigned, assigned ? "OFFLINE" : null);
    }

    public DeviceTelemetryTarget {
        if (deviceId == null) {
            throw new IllegalArgumentException("deviceId must not be null");
        }
        if (hardwareId == null || hardwareId.isBlank()) {
            throw new IllegalArgumentException("hardwareId must not be blank");
        }
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
    }

    /** Published status label used by LocalEdge to suppress telemetry in standby mode. */
    public boolean isStandby() {
        return "STANDBY".equals(status);
    }
}
