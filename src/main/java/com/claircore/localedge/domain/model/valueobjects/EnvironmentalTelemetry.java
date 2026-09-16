package com.claircore.localedge.domain.model.valueobjects;

import java.time.Instant;

/**
 * One simulated environmental reading produced by the LocalEdge generator. Plain types only so it
 * can cross into the ACL facades without dragging any framework dependency behind it.
 *
 * @param co2       CO₂ in ppm
 * @param temperature temperature in Celsius
 * @param humidity   relative humidity in %
 * @param pm1_0      PM1.0 in µg/m³
 * @param pm2_5      PM2.5 in µg/m³
 * @param pm10       PM10 in µg/m³
 * @param connectivityStatus "ONLINE" / "STANDBY" / etc. — string to match the device status enum
 * @param country    ISO country name
 * @param networkName free-form network identifier
 * @param recordedAt wall-clock instant the reading was produced
 */
public record EnvironmentalTelemetry(
        double co2,
        double temperature,
        double humidity,
        double pm1_0,
        double pm2_5,
        double pm10,
        String connectivityStatus,
        String country,
        String networkName,
        Instant recordedAt
) {
    public EnvironmentalTelemetry {
        if (connectivityStatus == null || connectivityStatus.isBlank()) {
            throw new IllegalArgumentException("connectivityStatus must not be blank");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("country must not be blank");
        }
        if (networkName == null || networkName.isBlank()) {
            throw new IllegalArgumentException("networkName must not be blank");
        }
        if (recordedAt == null) {
            throw new IllegalArgumentException("recordedAt must not be null");
        }
        if (!Double.isFinite(co2) || co2 < 0 || co2 > 1_000_000) {
            throw new IllegalArgumentException("co2 must be finite and within [0, 1e6] ppm");
        }
        if (!Double.isFinite(temperature) || temperature < -50 || temperature > 100) {
            throw new IllegalArgumentException("temperature must be finite and within [-50, 100] Celsius");
        }
        if (!Double.isFinite(humidity) || humidity < 0 || humidity > 100) {
            throw new IllegalArgumentException("humidity must be finite and within [0, 100] percent");
        }
        if (!Double.isFinite(pm1_0) || pm1_0 < 0 || pm1_0 > 10_000) {
            throw new IllegalArgumentException("pm1_0 must be finite and within [0, 10_000] µg/m³");
        }
        if (!Double.isFinite(pm2_5) || pm2_5 < 0 || pm2_5 > 10_000) {
            throw new IllegalArgumentException("pm2_5 must be finite and within [0, 10_000] µg/m³");
        }
        if (!Double.isFinite(pm10) || pm10 < 0 || pm10 > 10_000) {
            throw new IllegalArgumentException("pm10 must be finite and within [0, 10_000] µg/m³");
        }
    }
}
