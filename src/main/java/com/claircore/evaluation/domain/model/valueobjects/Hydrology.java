package com.claircore.evaluation.domain.model.valueobjects;

/**
 * Outdoor hydrology reading for one telemetry sample.
 *
 * <p>{@code rainfallMm} is precipitation accumulated in the sampling interval (mm).
 * {@code waterLevelCm} is absolute water level relative to the station datum (cm).
 * Both participate in device thresholds and alerting.
 */
public record Hydrology(Double rainfallMm, Double waterLevelCm) {
    public Hydrology {
        if (rainfallMm == null || !Double.isFinite(rainfallMm) || rainfallMm < 0 || rainfallMm > 1_000)
            throw new IllegalArgumentException("rainfallMm must be finite and between 0 and 1000 mm");
        if (waterLevelCm == null || !Double.isFinite(waterLevelCm) || waterLevelCm < 0 || waterLevelCm > 10_000)
            throw new IllegalArgumentException("waterLevelCm must be finite and between 0 and 10000 cm");
    }

    /** Indoor / legacy readings that omit hydrology default to a dry station at datum zero. */
    public static Hydrology absent() {
        return new Hydrology(0.0, 0.0);
    }
}
