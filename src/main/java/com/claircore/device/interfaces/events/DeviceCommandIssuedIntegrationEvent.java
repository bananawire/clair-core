package com.claircore.device.interfaces.events;

/**
 * Outbound command event used by integration adapters
 * {@code clair.device.commands.pending}.
 */
public record DeviceCommandIssuedIntegrationEvent(
        String commandId,
        String deviceId,
        String hardwareId,
        String commandType,
        String payload,
        String issuedAt
) {
}
