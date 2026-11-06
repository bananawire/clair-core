package com.claircore.nino.domain.model.valueobjects;

/**
 * Readings of one home; any field may be null when the caller does not have it.
 *
 * <p>Rainfall (mm in the sampling interval) and water level (cm over the station datum) come from
 * the outdoor hydrology sensors, with the same units as {@code evaluation}'s {@code Hydrology}.
 */
public record HomeReadings(Double humidity, Double temperature, String riskLevel, Integer riskScore,
                           Double rainfallMm, Double waterLevelCm) {

    public static HomeReadings empty() {
        return new HomeReadings(null, null, null, null, null, null);
    }
}
