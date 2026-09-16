package com.claircore.device.domain.model.valueobjects;

import java.time.Instant;
import java.util.UUID;

/**
 * One row of the edge roster: what a device is, whether it still exists, and the watermark the edge
 * pages on. A read model, produced by the repository, never stored.
 *
 * <p>{@code updatedAt} is the later of the device's and its assignment's own timestamp, because the
 * edge must re-read a device when either changes. It is the pagination cursor, so it must be the
 * value the row was actually ordered by — not a timestamp taken when the page was served.
 */
public record ProvisionedDevice(
        UUID deviceId,
        /** Current pairing generation, or null while the unit sits unclaimed in inventory. */
        UUID assignmentId,
        String hardwareId,
        String apiKey,
        DeviceStatus status,
        boolean deleted,
        Instant updatedAt
) {
    public ProvisionedDevice {
        if (deviceId == null) throw new IllegalArgumentException("Device ID must not be null");
        if (updatedAt == null) throw new IllegalArgumentException("updatedAt must not be null");
    }
}
