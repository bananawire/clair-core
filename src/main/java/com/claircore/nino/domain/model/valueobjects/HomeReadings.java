package com.claircore.nino.domain.model.valueobjects;

/** Readings of one home; any field may be null when the caller does not have it. */
public record HomeReadings(Double humidity, Double temperature, String riskLevel, Integer riskScore) {

    public static HomeReadings empty() {
        return new HomeReadings(null, null, null, null);
    }
}
