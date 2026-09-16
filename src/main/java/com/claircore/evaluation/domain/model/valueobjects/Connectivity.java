package com.claircore.evaluation.domain.model.valueobjects;
public record Connectivity(
        String status,
        String network,
        Integer signalStrength
) {
    public Connectivity {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status must not be null or blank");
        }
        if (signalStrength != null && (signalStrength < -150 || signalStrength > 0)) {
            throw new IllegalArgumentException("signalStrength must be between -150 and 0");
        }
    }
}
