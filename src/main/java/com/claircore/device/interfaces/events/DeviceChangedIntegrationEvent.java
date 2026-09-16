package com.claircore.device.interfaces.events;

/**
 * Outbound device change event used by integration adapters
 * {@code clair.provisioning.devices.changed}.
 */
public record DeviceChangedIntegrationEvent(
        String deviceId,
        String hardwareId,
        String apiKey,
        String status,
        String changeType,
        String changedAt
) {
}
