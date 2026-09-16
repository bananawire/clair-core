package com.claircore.alerting.interfaces.events;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Integration event emitted by clair-core for edge consumption.
 *
 * Edge/Embedded uses it to trigger local UX (LED/buzzer/screen) when an incident opens,
 * and to stop it when the incident closes.
 */
public record AlertIncidentChangedIntegrationEvent(
        UUID alertId,
        UUID deviceId,
        String hardwareId,
        UUID spaceId,
        String metric,
        BigDecimal thresholdValue,
        BigDecimal actualValue,
        String message,
        String status,
        Instant occurredAt,
        Instant resolvedAt
) {
}
