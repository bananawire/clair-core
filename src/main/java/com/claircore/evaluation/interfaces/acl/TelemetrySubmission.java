package com.claircore.evaluation.interfaces.acl;

import java.time.Instant;
import java.util.UUID;

/**
 * Inbound telemetry shape used by external producers (the LocalEdge bounded context, the
 * telemetry batch endpoint). Primitive fields only so the Evaluation BC can build its {@code
 * EvaluateTelemetryCommand} without crossing into other BCs.
 *
 * <p>All metric fields are doubles in the indoor product range; {@code status} is a free-form
 * string the device firmware uses (normally {@code "OK"}), and {@code connectivityStatus} the
 * receive side stores verbatim.
 */
public record TelemetrySubmission(
        UUID deviceId,
        double co2,
        double temperature,
        double humidity,
        double pm1_0,
        double pm2_5,
        double pm10,
        int healthStatus,
        String status,
        String country,
        String networkName,
        String connectivityStatus,
        Integer signalStrength,
        Instant recordedAt
) {
    public TelemetrySubmission {
        if (deviceId == null) {
            throw new IllegalArgumentException("deviceId must not be null");
        }
        if (recordedAt == null) {
            throw new IllegalArgumentException("recordedAt must not be null");
        }
        if (connectivityStatus == null || connectivityStatus.isBlank()) {
            throw new IllegalArgumentException("connectivityStatus must not be blank");
        }
        if (healthStatus < 0 || healthStatus > 100) {
            throw new IllegalArgumentException("healthStatus must be within [0, 100]");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status must not be blank");
        }
    }
}
