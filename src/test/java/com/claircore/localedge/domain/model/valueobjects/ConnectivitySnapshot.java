package com.claircore.localedge.domain.model.valueobjects;

import java.time.Instant;

/**
 * Connectivity snapshot for a single LocalEdge cycle. Always ONLINE — the LocalEdge is the
 * source of telemetry, not a downstream consumer reporting on connectivity events.
 */
public record ConnectivitySnapshot(
        String status,
        String networkName,
        Integer signalStrength,
        Instant observedAt
) {
    public ConnectivitySnapshot {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status must not be blank");
        }
        if (networkName == null || networkName.isBlank()) {
            throw new IllegalArgumentException("networkName must not be blank");
        }
        if (signalStrength != null && (signalStrength < -150 || signalStrength > 0)) {
            throw new IllegalArgumentException("signalStrength must be within [-150, 0] dBm");
        }
        if (observedAt == null) {
            throw new IllegalArgumentException("observedAt must not be null");
        }
    }
}
