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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Realistic indoor air-quality simulator. Each metric (PM2.5, CO₂, temperature, humidity)
 * follows an AR(1) process with a sinusoidal seasonal mean and occasional exogenous shocks:
 *
 * <pre>
 *   X_t   = μ_t + φ·(X_{t−1} − μ_{t−1}) + β·E_t + ε_t,    ε_t ∼ N(0, σ²)
 *   μ_t   = B + A·sin(2π·t/T + θ + φ_offset)
 * </pre>
 *
 * <p><b>Why the AR(1) form kills the "jump" problem.</b> The standard random-walk generator
 * drew each reading uniformly inside a band, so a single cycle could jump from PM2.5 = 8
 * (RECOMMENDED) to PM2.5 = 600 (HIGH). Here, the {@code φ·(X_{t−1} − μ_{t−1})} term keeps
 * the trajectory close to the previous reading: when {@code X_{t−1} ≈ μ_{t−1}} the next
 * step is just {@code μ_t + β·E_t + ε}, and {@code φ < 1} forces any spike (cooking,
 * occupancy) to decay back to the seasonal mean over a handful of cycles.
 *
 * <p><b>What each knob does.</b>
 * <ul>
 *   <li>{@code B, A, T, θ}: the seasonal mean. Defaults give a 1-hour period (T = 240 cycles
 *       at the 15 s cadence), so a demo run sees the full swing in roughly an hour.</li>
 *   <li>{@code φ}: persistence ∈ (0, 1). 0.85 for PM2.5, 0.92 for CO₂, 0.96 for temperature
 *       — temperature is the most sluggish of the four.</li>
 *   <li>{@code σ}: Gaussian noise std. 1 µg/m³ PM2.5, 18 ppm CO₂, 0.05 °C, 0.4 % humidity.</li>
 *   <li>{@code E_t, β}: external shocks. Bernoulli({@code eventProb}) fires cooking
 *       (PM2.5) or occupancy (CO₂) bumps that decay naturally through the AR term.</li>
 * </ul>
 *
 * <p><b>Determinism.</b> With {@code seed != 0} the shared {@link java.util.Random} is fixed
 * and two fresh policies with the same seed produce identical first-call readings for the
 * same {@code deviceId} and {@code scenario}. With {@code seed == 0} the generator uses a
 * fresh {@link SecureRandom} on every cycle, so successive calls are not predictable.
 *
 * <p><b>Statefulness.</b> The policy is a Spring singleton bean; the
 * {@code Map<UUID, DeviceState>} is what makes consecutive cycles evolve smoothly instead
 * of redrawing from scratch each cycle. Switching scenarios reseeds the state at the new
 * scenario's seasonal mean so a hot toggle doesn't leave the device oscillating around the
 * old mean forever.
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

    private enum Metric { PM25, CO2, TEMP, HUMIDITY }

    /**
     * AR(1) parameters for one (scenario, metric) cell. Values come from
     * {@link #params(SimulationScenario, Metric)}; held in a record so the AR step has a
     * single typed bundle to read from.
     */
    private record MetricParams(
            double base,          // B   — seasonal mean center
            double amplitude,     // A   — half-range of the seasonal swing
            double period,        // T   — cycles per full sine period
            double phase,         // θ   — scenario phase offset (radians)
            double phi,           // φ   — AR(1) persistence, 0 < φ < 1
            double sigma,         // σ   — Gaussian noise std
            double eventProb,     // P(E_t = 1) per cycle
            double eventMagnitude // β   — magnitude of the event shock
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

        double pm1 = state.pm25 * (0.40 + 0.15 * random.nextDouble());  // strictly below pm25
        double pm10 = state.pm25 * (1.20 + 0.30 * random.nextDouble()); // strictly above pm25
        int dbm = -1 * (40 + random.nextInt(31));                       // -40 to -70 dBm

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
                connectivity.status(),
                location.country(),
                connectivity.networkName(),
                now);
    }

    /**
     * Single AR(1) step. {@code mu_t} is the seasonal mean at the current cycle,
     * {@code muPrev} at the previous cycle. When {@code t == 0} both equal the initial
     * mean and the persistence term collapses, giving the steady-start behaviour the tests
     * rely on.
     */
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

    /**
     * Per-(scenario, metric) AR(1) parameters. {@code base} sits inside the target indoor
     * band so the seasonal mean oscillates around healthy, alert, or hazardous values; the
     * amplitudes are bounded so peak-to-peak swings stay plausible for indoor air quality.
     */
    private MetricParams params(SimulationScenario s, Metric m) {
        return switch (s) {
            case MIXED -> switch (m) {
                case PM25 -> new MetricParams(12.0, 6.0, 240.0, 0.0, 0.85, 1.0, 0.015, 25.0);
                // CO₂ bounds: with φ=0.85 the stationary spread is σ / sqrt(1-φ²) ≈ 2.3σ,
                // so the seasonal min (B − A = 500) minus 3σ_stationary stays above 400.
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
        return s;
    }

    private void reseedState(DeviceState state, UUID deviceId, SimulationScenario scenario) {
        DeviceState fresh = initialState(deviceId, scenario);
        state.pm25 = fresh.pm25;
        state.co2 = fresh.co2;
        state.temperature = fresh.temperature;
        state.humidity = fresh.humidity;
        state.lastScenario = scenario;
        // Keep cycleCount so the seasonal phase continues across the toggle; the AR term
        // will move the values toward the new scenario's mean within a handful of cycles.
    }

    /** Per-device phase offset (0 to 2π) so devices don't all rise and fall in lock-step. */
    private double phaseOffset(UUID deviceId) {
        long h = deviceId.getLeastSignificantBits() ^ deviceId.getMostSignificantBits();
        return (h & 0xFFFFL) / 65535.0 * TWO_PI;
    }

    /** Box–Muller; consumes two uniform draws. {@code Math.max} guards the log() against u1=0. */
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
        SimulationScenario lastScenario;
        long cycleCount;

        DeviceState(SimulationScenario scenario, long cycleCount) {
            this.lastScenario = scenario;
            this.cycleCount = cycleCount;
        }
    }
}
