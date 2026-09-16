package com.claircore.localedge.domain.services;

import com.claircore.localedge.domain.model.valueobjects.ConnectivitySnapshot;
import com.claircore.localedge.domain.model.valueobjects.EnvironmentalSeverity;
import com.claircore.localedge.domain.model.valueobjects.EnvironmentalTelemetry;
import com.claircore.localedge.domain.model.valueobjects.LocationSnapshot;
import com.claircore.localedge.domain.model.valueobjects.SimulationScenario;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Pure-domain generator that turns a seeded scenario into a list of {@link EnvironmentalTelemetry}
 * readings. Pure logic, no Spring, no IO — what the bounded context hashes for tests.
 *
 * <p>Threshold bands (Peru, indoor air quality reference):
 * <ul>
 *   <li>{@link EnvironmentalSeverity#RECOMMENDED} — PM2.5 ∈ [0,25], CO₂ ∈ [400,800],
 *       temperature ∈ [22,26], humidity ∈ [40,60]</li>
 *   <li>{@link EnvironmentalSeverity#ALERT}       — PM2.5 ∈ (25,50], CO₂ ∈ (800,1000],
 *       temperature ∈ [18,22)∪(26,28], humidity ∈ [35,40)∪(60,65]</li>
 *   <li>{@link EnvironmentalSeverity#HIGH}        — PM2.5 ∈ (50,1000], CO₂ ∈ (1000,5000],
 *       temperature ∈ [-10,18)∪(28,60], humidity ∈ [0,35)∪(65,100]</li>
 * </ul>
 *
 * <p>Determinism: with {@code seed != 0} the generator uses a {@link java.util.Random}; with
 * {@code seed == 0} it falls back to a fresh {@link SecureRandom} on every call so successive
 * cycles runs are not predictable. PM1.0 is sampled strictly below PM2.5; PM10 strictly above;
 * band transitions flip between adjacent gauges to keep indoor plausible.
 */
public class SyntheticTelemetryGeneratorPolicy {

    /** Indoor product accepts PM2.5 up to 1000 µg/m³ without overflow off the band table. */
    private static final double PM_HIGH_CEILING = 1000.0;
    private static final double CO2_HIGH_CEILING = 5000.0;
    /** Fixed labels — the LocalEdge is a closed simulator; do not let them drift. */
    static final String DEFAULT_NETWORK_NAME = "LOCAL-SIMULATOR";
    static final String DEFAULT_COUNTRY = "Peru";
    static final String DEFAULT_CONNECTIVITY_STATUS = "ONLINE";
    /** Synthetic healthy signal: 100 means "operating normally" in the receive-side model. */
    private static final int HEALTH_RECOMMENDED = 95;
    private static final int HEALTH_ALERT = 70;
    private static final int HEALTH_HIGH = 40;

    private final java.util.Random random;

    public SyntheticTelemetryGeneratorPolicy(long seed) {
        this.random = seed == 0L ? new SecureRandom() : new java.util.Random(seed);
    }

    /** Hidden constructor used by tests to inject a deterministic Random. */
    SyntheticTelemetryGeneratorPolicy(java.util.Random random) {
        this.random = random;
    }

    /**
     * Generates a list of {@code count} readings, one per device id, at the given instant.
     * {@code count} and {@code deviceIds.size()} must agree.
     */
    public List<EnvironmentalTelemetry> generate(
            List<UUID> deviceIds,
            SimulationScenario scenario,
            Instant now) {
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        if (scenario == null) {
            throw new IllegalArgumentException("scenario must not be null");
        }
        if (deviceIds == null) {
            throw new IllegalArgumentException("deviceIds must not be null");
        }
        List<EnvironmentalTelemetry> readings = new ArrayList<>(deviceIds.size());
        for (UUID deviceId : deviceIds) {
            readings.add(generateOne(scenario, now));
        }
        return List.copyOf(readings);
    }

    /** Single-device convenience used directly by tests. */
    public EnvironmentalTelemetry generateOne(SimulationScenario scenario, Instant now) {
        EnvironmentalSeverity severity = pickSeverity(scenario);
        double pm25 = pickPm25(severity);
        double co2 = pickCo2(severity);
        double temperature = pickTemperature(severity);
        double humidity = pickHumidity(severity);
        double pm1 = pm25 * (0.40 + 0.15 * random.nextDouble()); // strictly below pm25
        double pm10 = pm25 * (1.20 + 0.30 * random.nextDouble()); // strictly above pm25

        ConnectivitySnapshot connectivity = new ConnectivitySnapshot(
                DEFAULT_CONNECTIVITY_STATUS,
                DEFAULT_NETWORK_NAME,
                Integer.valueOf(-1 * (40 + random.nextInt(31))), // -40 to -70 dBm
                now);
        LocationSnapshot location = new LocationSnapshot(DEFAULT_COUNTRY, now);

        return new EnvironmentalTelemetry(
                round2(co2),
                round2(temperature),
                round2(humidity),
                round2(pm1),
                round2(pm25),
                round2(pm10),
                connectivity.status(),
                location.country(),
                connectivity.networkName(),
                now);
    }

    /**
     * Severity to score on this draw; the scenario weights the three bands and we
     * pick whichever cumulative bucket the dice falls into.
     */
    private EnvironmentalSeverity pickSeverity(SimulationScenario scenario) {
        double r = random.nextDouble();
        double recommended = scenario.recommendedShare();
        double alert = scenario.alertShare() + recommended;
        if (r < recommended) return EnvironmentalSeverity.RECOMMENDED;
        if (r < alert) return EnvironmentalSeverity.ALERT;
        return EnvironmentalSeverity.HIGH;
    }

    /** Uniform draw within the matching PM2.5 interval (µg/m³). */
    private double pickPm25(EnvironmentalSeverity s) {
        return switch (s) {
            case RECOMMENDED -> between(0.0, 25.0);
            case ALERT       -> between(25.0001, 50.0);
            case HIGH        -> between(50.0001, PM_HIGH_CEILING);
        };
    }

    /** Uniform draw within the matching CO₂ interval (ppm). */
    private double pickCo2(EnvironmentalSeverity s) {
        return switch (s) {
            case RECOMMENDED -> between(400.0, 800.0);
            case ALERT       -> between(800.0001, 1000.0);
            case HIGH        -> between(1000.0001, CO2_HIGH_CEILING);
        };
    }

    /** Uniform draw within the matching temperature interval (Celsius). */
    private double pickTemperature(EnvironmentalSeverity s) {
        return switch (s) {
            case RECOMMENDED -> between(22.0, 26.0);
            case ALERT       -> {
                // [18,22) ∪ (26,28]; pick which side by the dice to keep distribution uniform.
                yield random.nextBoolean()
                        ? between(18.0, 21.9999)
                        : between(26.0001, 28.0);
            }
            case HIGH        -> {
                // [-10,18) ∪ (28,60]
                yield random.nextBoolean()
                        ? between(-10.0, 17.9999)
                        : between(28.0001, 60.0);
            }
        };
    }

    /** Uniform draw within the matching humidity interval (%). */
    private double pickHumidity(EnvironmentalSeverity s) {
        return switch (s) {
            case RECOMMENDED -> between(40.0, 60.0);
            case ALERT       -> {
                // [35,40) ∪ (60,65]
                yield random.nextBoolean()
                        ? between(35.0, 39.9999)
                        : between(60.0001, 65.0);
            }
            case HIGH        -> {
                // [0,35) ∪ (65,100]
                yield random.nextBoolean()
                        ? between(0.0, 34.9999)
                        : between(65.0001, 100.0);
            }
        };
    }

    /** Health gauge the evaluation BC will read alongside the metrics: HIGH < ALERT < RECOMMENDED. */
    public static int healthFor(EnvironmentalSeverity severity) {
        return switch (severity) {
            case RECOMMENDED -> HEALTH_RECOMMENDED;
            case ALERT -> HEALTH_ALERT;
            case HIGH -> HEALTH_HIGH;
        };
    }

    private double between(double lo, double hi) {
        if (hi <= lo) {
            throw new IllegalStateException("invalid range [" + lo + ", " + hi + "]");
        }
        return lo + random.nextDouble() * (hi - lo);
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /** Locale-stable factory for logs. */
    public static String formatCountry() {
        return DEFAULT_COUNTRY;
    }

    @Override
    public String toString() {
        return "SyntheticTelemetryGeneratorPolicy{random=" + random.getClass().getSimpleName() + "}";
    }

    /** Internal helper used by tests that want to drive the random directly. */
    java.util.Random randomForTests() {
        return random;
    }

    /** Returns the network name in use; exposed for diagnostic messages. */
    public String networkName() {
        return DEFAULT_NETWORK_NAME;
    }

    /** Stable string used by tests to assert against {@link Locale}. */
    public static String countryString() {
        return DEFAULT_COUNTRY;
    }
}
