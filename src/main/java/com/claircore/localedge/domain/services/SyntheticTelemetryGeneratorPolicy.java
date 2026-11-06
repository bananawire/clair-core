package com.claircore.localedge.domain.services;

import com.claircore.localedge.domain.model.valueobjects.ConnectivitySnapshot;
import com.claircore.localedge.domain.model.valueobjects.EnvironmentalSeverity;
import com.claircore.localedge.domain.model.valueobjects.EnvironmentalTelemetry;
import com.claircore.localedge.domain.model.valueobjects.LocationSnapshot;
import com.claircore.localedge.domain.model.valueobjects.SimulationScenario;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Realistic indoor/outdoor air-quality and hydrology simulator.
 *
 * <p>Air metrics (PM2.5, CO₂, temperature, humidity) follow an AR(1) process with a sinusoidal
 * seasonal mean and occasional exogenous shocks:
 *
 * <pre>
 *   X_t   = μ_t + φ·(X_{t−1} − μ_{t−1}) + β·E_t + ε_t,    ε_t ∼ N(0, σ²)
 *   μ_t   = B + A·sin(2π·t/T + θ + φ_offset)
 * </pre>
 *
 * <p>Outdoor hydrology / power fields are physically coupled:
 * <ul>
 *   <li>{@code rainfallMm}: Markov wet/dry chain + Gamma intensity, converted over Δt.</li>
 *   <li>{@code waterLevelCm}: storage mass balance with a one-cycle lag of rain runoff.</li>
 *   <li>{@code batteryPercent} / {@code solarCharging}: PVWatts-style irradiance reduced by
 *       cloud/rain cover, then Coulomb / energy balance against a fixed ESP32 load.</li>
 * </ul>
 */
public class SyntheticTelemetryGeneratorPolicy {

    static final String DEFAULT_NETWORK_NAME = "LOCAL-SIMULATOR";
    static final String DEFAULT_COUNTRY = "Peru";
    static final String DEFAULT_CONNECTIVITY_STATUS = "ONLINE";

    private static final int HEALTH_RECOMMENDED = 95;
    private static final int HEALTH_ALERT = 70;
    private static final int HEALTH_HIGH = 40;

    private static final double TWO_PI = 2.0 * Math.PI;
    private static final UUID LEGACY_DEVICE_ID = new UUID(0L, 0L);

    /** Sampling interval assumed by the LocalEdge scheduler (15 s). */
    private static final double DELTA_T_SECONDS = 15.0;
    private static final double DELTA_T_HOURS = DELTA_T_SECONDS / 3600.0;

    // Hydrology mass-balance knobs (small synthetic catchment).
    private static final double CATCHMENT_AREA_M2 = 2_500.0;
    private static final double RUNOFF_COEFFICIENT = 0.45;
    private static final double STORAGE_AREA_M2 = 120.0;
    private static final double OUTFLOW_K = 0.012;
    private static final double OUTFLOW_EXPONENT = 1.5;
    private static final double BASE_WATER_LEVEL_M = 0.40;
    private static final int RAIN_LAG_CYCLES = 1;

    // Power knobs.
    private static final double PANEL_STC_W = 10.0;
    private static final double PANEL_TEMP_COEFF = -0.004;
    private static final double REGULATOR_EFFICIENCY = 0.80;
    private static final double BATTERY_CAPACITY_WH = 18.5;
    private static final double ESP32_LOAD_W = 0.6;
    private static final double CHARGE_EFFICIENCY = 0.90;
    private static final double DISCHARGE_EFFICIENCY = 0.95;

    private enum Metric { PM25, CO2, TEMP, HUMIDITY }

    private record MetricParams(
            double base,
            double amplitude,
            double period,
            double phase,
            double phi,
            double sigma,
            double eventProb,
            double eventMagnitude
    ) {}

    private final java.util.Random random;
    private final Map<UUID, DeviceState> stateByDevice = new ConcurrentHashMap<>();

    public SyntheticTelemetryGeneratorPolicy(long seed) {
        this.random = seed == 0L ? new SecureRandom() : new java.util.Random(seed);
    }

    /** Hidden constructor used by tests to inject a deterministic {@link java.util.Random}. */
    SyntheticTelemetryGeneratorPolicy(java.util.Random random) {
        this.random = random;
    }

    public List<EnvironmentalTelemetry> generate(
            List<UUID> deviceIds, SimulationScenario scenario, Instant now) {
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
            readings.add(generateOne(deviceId, scenario, now));
        }
        return List.copyOf(readings);
    }

    /**
     * Backward-compatible single-device entry point for callers that do not have an inventory id.
     * New LocalEdge flows should use the device-aware overload so state is isolated per device.
     */
    public EnvironmentalTelemetry generateOne(SimulationScenario scenario, Instant now) {
        return generateOne(LEGACY_DEVICE_ID, scenario, now);
    }

    public EnvironmentalTelemetry generateOne(UUID deviceId, SimulationScenario scenario, Instant now) {
        if (deviceId == null) {
            throw new IllegalArgumentException("deviceId must not be null");
        }
        if (scenario == null) {
            throw new IllegalArgumentException("scenario must not be null");
        }
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }

        DeviceState state = stateByDevice.computeIfAbsent(deviceId, id -> initialState(id, scenario));
        if (state.lastScenario != scenario) {
            reseedState(state, deviceId, scenario);
        }

        long t = state.cycleCount;
        long tPrev = Math.max(0L, t - 1L);
        double phaseOffset = phaseOffset(deviceId);

        state.pm25 = arStep(state.pm25, t, tPrev, params(scenario, Metric.PM25), phaseOffset);
        state.co2 = arStep(state.co2, t, tPrev, params(scenario, Metric.CO2), phaseOffset);
        state.temperature = arStep(state.temperature, t, tPrev, params(scenario, Metric.TEMP), phaseOffset);
        state.humidity = arStep(state.humidity, t, tPrev, params(scenario, Metric.HUMIDITY), phaseOffset);

        stepHydrologyAndPower(state, scenario, now);

        double pm1 = state.pm25 * (0.40 + 0.15 * random.nextDouble());
        double pm10 = state.pm25 * (1.20 + 0.30 * random.nextDouble());
        int dbm = -1 * (40 + random.nextInt(31));

        ConnectivitySnapshot connectivity = new ConnectivitySnapshot(
                DEFAULT_CONNECTIVITY_STATUS, DEFAULT_NETWORK_NAME, Integer.valueOf(dbm), now);
        LocationSnapshot location = new LocationSnapshot(DEFAULT_COUNTRY, now);

        state.cycleCount++;
        state.lastScenario = scenario;

        return new EnvironmentalTelemetry(
                round2(state.co2),
                round2(state.temperature),
                round2(state.humidity),
                round2(pm1),
                round2(state.pm25),
                round2(pm10),
                round2(state.rainfallMm),
                round2(state.waterLevelCm),
                round2(state.batteryPercent),
                state.solarCharging,
                connectivity.status(),
                location.country(),
                connectivity.networkName(),
                now);
    }

    private void stepHydrologyAndPower(DeviceState state, SimulationScenario scenario, Instant now) {
        RainParams rain = rainParams(scenario);
        boolean wasWet = state.wet;
        double stayWetProb = wasWet ? rain.p11 : rain.p01;
        state.wet = random.nextDouble() < stayWetProb;

        double intensityMmPerHour = 0.0;
        if (state.wet) {
            intensityMmPerHour = sampleGamma(rain.gammaShape, rain.gammaScale);
        }
        state.rainfallMm = Math.max(0.0, intensityMmPerHour * DELTA_T_HOURS);

        // One-cycle lag: runoff from the previous interval arrives now.
        double laggedRainMm = state.pendingRunoffMm;
        state.pendingRunoffMm = state.rainfallMm;

        double qIn = (RUNOFF_COEFFICIENT * CATCHMENT_AREA_M2 * (laggedRainMm / DELTA_T_HOURS)) / 3_600_000.0;
        double headAboveBase = Math.max(0.0, (state.waterLevelCm / 100.0) - BASE_WATER_LEVEL_M);
        double qOut = OUTFLOW_K * Math.pow(headAboveBase, OUTFLOW_EXPONENT);
        double deltaHMeters = ((qIn - qOut) * DELTA_T_SECONDS) / STORAGE_AREA_M2;
        state.waterLevelCm = clamp(state.waterLevelCm + deltaHMeters * 100.0, 5.0, 500.0);

        int hour = now.atZone(ZoneOffset.UTC).getHour();
        double clearSky = clearSkyIrradiance(hour);
        double cloudFactor = state.wet ? Math.max(0.05, 1.0 - Math.min(1.0, intensityMmPerHour / 40.0)) : 1.0;
        double irradiance = clearSky * cloudFactor;
        double cellTemp = state.temperature + (irradiance > 50.0 ? 10.0 : 0.0);
        double pPv = PANEL_STC_W * (irradiance / 1000.0) * (1.0 + PANEL_TEMP_COEFF * (cellTemp - 25.0));
        pPv = Math.max(0.0, pPv);
        double usableSolar = REGULATOR_EFFICIENCY * pPv;
        double netPower = usableSolar - ESP32_LOAD_W;

        double energyWh = state.batteryPercent / 100.0 * BATTERY_CAPACITY_WH;
        if (netPower >= 0.0) {
            energyWh += CHARGE_EFFICIENCY * netPower * DELTA_T_HOURS;
        } else {
            energyWh += (netPower / DISCHARGE_EFFICIENCY) * DELTA_T_HOURS;
        }
        energyWh = clamp(energyWh, 0.0, BATTERY_CAPACITY_WH);
        state.batteryPercent = 100.0 * energyWh / BATTERY_CAPACITY_WH;
        state.solarCharging = usableSolar > ESP32_LOAD_W && state.batteryPercent < 99.9;
    }

    private static double clearSkyIrradiance(int hourUtc) {
        // Simple daylight envelope peaking near local noon for coastal Peru (UTC-5 ≈ hour 17 UTC).
        int localHour = (hourUtc - 5 + 24) % 24;
        if (localHour < 6 || localHour > 18) {
            return 0.0;
        }
        double x = (localHour - 6) / 12.0;
        return 1000.0 * Math.sin(Math.PI * x);
    }

    /** Marsaglia–Tsang style Gamma(k, θ) for k >= 1; for k < 1 boosts via Gamma(k+1)/U^{1/k}. */
    private double sampleGamma(double shape, double scale) {
        if (shape < 1.0) {
            double boosted = sampleGamma(shape + 1.0, scale);
            double u = Math.max(random.nextDouble(), 1e-12);
            return boosted * Math.pow(u, 1.0 / shape);
        }
        double d = shape - 1.0 / 3.0;
        double c = 1.0 / Math.sqrt(9.0 * d);
        while (true) {
            double x = gaussian(1.0);
            double v = 1.0 + c * x;
            if (v <= 0) continue;
            v = v * v * v;
            double u = random.nextDouble();
            if (u < 1.0 - 0.0331 * (x * x) * (x * x)) {
                return d * v * scale;
            }
            if (Math.log(u) < 0.5 * x * x + d * (1.0 - v + Math.log(v))) {
                return d * v * scale;
            }
        }
    }

    private double arStep(double xPrev, long t, long tPrev, MetricParams p, double phaseOffset) {
        double muT = seasonalMean(p, t, phaseOffset);
        double muPrev = seasonalMean(p, tPrev, phaseOffset);
        double event = (random.nextDouble() < p.eventProb) ? p.eventMagnitude : 0.0;
        double noise = gaussian(p.sigma);
        return muT + p.phi * (xPrev - muPrev) + event + noise;
    }

    private static double seasonalMean(MetricParams p, long cycle, double phaseOffset) {
        return p.base + p.amplitude * Math.sin(TWO_PI * cycle / p.period + p.phase + phaseOffset);
    }

    private MetricParams params(SimulationScenario s, Metric m) {
        return switch (s) {
            case MIXED -> switch (m) {
                case PM25 -> new MetricParams(12.0, 6.0, 240.0, 0.0, 0.85, 1.0, 0.015, 25.0);
                case CO2  -> new MetricParams(600.0, 100.0, 240.0, 0.0, 0.85, 12.0, 0.025, 250.0);
                case TEMP -> new MetricParams(23.5, 1.5, 240.0, 0.0, 0.96, 0.05, 0.0, 0.0);
                case HUMIDITY -> new MetricParams(50.0, 8.0, 240.0, 0.0, 0.90, 0.4, 0.0, 0.0);
            };
            case RECOMMENDED_ONLY -> switch (m) {
                case PM25 -> new MetricParams(8.0, 3.0, 240.0, 0.0, 0.90, 0.7, 0.0, 0.0);
                case CO2  -> new MetricParams(550.0, 100.0, 240.0, 0.0, 0.94, 12.0, 0.0, 0.0);
                case TEMP -> new MetricParams(23.5, 1.0, 240.0, 0.0, 0.97, 0.04, 0.0, 0.0);
                case HUMIDITY -> new MetricParams(50.0, 5.0, 240.0, 0.0, 0.92, 0.3, 0.0, 0.0);
            };
            case ALERT_ONLY -> switch (m) {
                case PM25 -> new MetricParams(37.5, 6.0, 240.0, 0.0, 0.85, 1.5, 0.04, 30.0);
                case CO2  -> new MetricParams(900.0, 80.0, 240.0, 0.0, 0.92, 20.0, 0.06, 250.0);
                case TEMP -> new MetricParams(27.0, 0.5, 240.0, 0.0, 0.96, 0.05, 0.0, 0.0);
                case HUMIDITY -> new MetricParams(62.0, 1.5, 240.0, 0.0, 0.90, 0.3, 0.0, 0.0);
            };
            case HIGH_ONLY -> switch (m) {
                case PM25 -> new MetricParams(200.0, 100.0, 240.0, 0.0, 0.85, 10.0, 0.10, 150.0);
                case CO2  -> new MetricParams(2000.0, 500.0, 240.0, 0.0, 0.92, 50.0, 0.08, 500.0);
                case TEMP -> new MetricParams(32.0, 2.0, 240.0, 0.0, 0.96, 0.1, 0.0, 0.0);
                case HUMIDITY -> new MetricParams(80.0, 8.0, 240.0, 0.0, 0.90, 0.5, 0.0, 0.0);
            };
            case STRESS_TEST -> switch (m) {
                case PM25 -> new MetricParams(150.0, 80.0, 240.0, 0.0, 0.80, 12.0, 0.20, 200.0);
                case CO2  -> new MetricParams(1800.0, 400.0, 240.0, 0.0, 0.88, 60.0, 0.20, 600.0);
                case TEMP -> new MetricParams(30.0, 3.0, 240.0, 0.0, 0.94, 0.2, 0.0, 0.0);
                case HUMIDITY -> new MetricParams(78.0, 10.0, 240.0, 0.0, 0.88, 0.6, 0.0, 0.0);
            };
        };
    }

    private record RainParams(double p01, double p11, double gammaShape, double gammaScale) {}

    private RainParams rainParams(SimulationScenario scenario) {
        return switch (scenario) {
            case RECOMMENDED_ONLY -> new RainParams(0.01, 0.20, 1.5, 2.0);
            case MIXED -> new RainParams(0.04, 0.55, 2.0, 4.0);
            case ALERT_ONLY -> new RainParams(0.08, 0.70, 2.2, 6.0);
            case HIGH_ONLY -> new RainParams(0.15, 0.80, 2.5, 10.0);
            case STRESS_TEST -> new RainParams(0.25, 0.90, 3.0, 14.0);
        };
    }

    private DeviceState initialState(UUID deviceId, SimulationScenario scenario) {
        double phaseOffset = phaseOffset(deviceId);
        DeviceState s = new DeviceState(scenario, 0L);
        for (Metric metric : Metric.values()) {
            MetricParams p = params(scenario, metric);
            double v = seasonalMean(p, 0L, phaseOffset);
            switch (metric) {
                case PM25 -> s.pm25 = v;
                case CO2 -> s.co2 = v;
                case TEMP -> s.temperature = v;
                case HUMIDITY -> s.humidity = v;
            }
        }
        s.waterLevelCm = BASE_WATER_LEVEL_M * 100.0;
        s.batteryPercent = 70.0;
        s.solarCharging = false;
        s.wet = false;
        s.pendingRunoffMm = 0.0;
        s.rainfallMm = 0.0;
        return s;
    }

    private void reseedState(DeviceState state, UUID deviceId, SimulationScenario scenario) {
        DeviceState fresh = initialState(deviceId, scenario);
        state.pm25 = fresh.pm25;
        state.co2 = fresh.co2;
        state.temperature = fresh.temperature;
        state.humidity = fresh.humidity;
        state.waterLevelCm = fresh.waterLevelCm;
        state.batteryPercent = fresh.batteryPercent;
        state.solarCharging = fresh.solarCharging;
        state.wet = fresh.wet;
        state.pendingRunoffMm = fresh.pendingRunoffMm;
        state.rainfallMm = fresh.rainfallMm;
        state.lastScenario = scenario;
    }

    private double phaseOffset(UUID deviceId) {
        long h = deviceId.getLeastSignificantBits() ^ deviceId.getMostSignificantBits();
        return (h & 0xFFFFL) / 65535.0 * TWO_PI;
    }

    private double gaussian(double sigma) {
        double u1 = Math.max(random.nextDouble(), 1e-12);
        double u2 = random.nextDouble();
        return sigma * Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(TWO_PI * u2);
    }

    public static int healthFor(EnvironmentalSeverity severity) {
        return switch (severity) {
            case RECOMMENDED -> HEALTH_RECOMMENDED;
            case ALERT -> HEALTH_ALERT;
            case HIGH -> HEALTH_HIGH;
        };
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    public static String formatCountry() {
        return DEFAULT_COUNTRY;
    }

    public String networkName() {
        return DEFAULT_NETWORK_NAME;
    }

    public static String countryString() {
        return DEFAULT_COUNTRY;
    }

    @Override
    public String toString() {
        return "SyntheticTelemetryGeneratorPolicy{random=" + random.getClass().getSimpleName()
                + ", tracked=" + stateByDevice.size() + "}";
    }

    java.util.Random randomForTests() {
        return random;
    }

    private static final class DeviceState {
        double pm25;
        double co2;
        double temperature;
        double humidity;
        double rainfallMm;
        double waterLevelCm;
        double batteryPercent;
        boolean solarCharging;
        boolean wet;
        double pendingRunoffMm;
        SimulationScenario lastScenario;
        long cycleCount;

        DeviceState(SimulationScenario scenario, long cycleCount) {
            this.lastScenario = scenario;
            this.cycleCount = cycleCount;
        }
    }
}
