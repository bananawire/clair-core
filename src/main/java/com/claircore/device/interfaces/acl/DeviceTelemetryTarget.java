package com.claircore.device.interfaces.acl;

import java.util.UUID;

/**
 * Picked-up-by-the-edge view of a device: the LocalEdge schedules against this shape, never
 * against the full Device aggregate. {@code assigned} is true if a {@code DeviceAssignment} row
 * exists; the LocalEdge defaults to {@code includeUnassigned=false} because unpaired devices
 * have no owner to send push notifications to.
 */
public record DeviceTelemetryTarget(
        UUID deviceId,
        String hardwareId,
        String name,
        boolean assigned
) {
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
}
