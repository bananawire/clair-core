package com.claircore.localedge.domain.services;

import com.claircore.localedge.domain.model.valueobjects.EnvironmentalSeverity;
import com.claircore.localedge.domain.model.valueobjects.EnvironmentalTelemetry;
import com.claircore.localedge.domain.model.valueobjects.SimulationScenario;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SyntheticTelemetryGeneratorPolicyTest {

    private static final Instant NOW = Instant.parse("2026-05-16T22:30:00Z");
    private static final UUID DEVICE_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID DEVICE_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Test
    void deterministicSeedProducesTheSameReadingEveryTime() {
        long seed = 42L;
        EnvironmentalTelemetry a = new SyntheticTelemetryGeneratorPolicy(seed)
                .generateOne(DEVICE_A, SimulationScenario.MIXED, NOW);
        EnvironmentalTelemetry b = new SyntheticTelemetryGeneratorPolicy(seed)
                .generateOne(DEVICE_A, SimulationScenario.MIXED, NOW);

        assertThat(a).isEqualTo(b);
    }

    @Test
    void differentSeedsProduceDifferentReadings() {
        EnvironmentalTelemetry a = new SyntheticTelemetryGeneratorPolicy(1L)
                .generateOne(DEVICE_A, SimulationScenario.MIXED, NOW);
        EnvironmentalTelemetry b = new SyntheticTelemetryGeneratorPolicy(2L)
                .generateOne(DEVICE_A, SimulationScenario.MIXED, NOW);

        assertThat(a.pm2_5()).isNotEqualTo(b.pm2_5());
    }

    @Test
    void mixedScenarioStaysInsideTheIndoorRange() {
        SyntheticTelemetryGeneratorPolicy policy = new SyntheticTelemetryGeneratorPolicy(7L);
        for (int i = 0; i < 200; i++) {
            EnvironmentalTelemetry reading = policy.generateOne(DEVICE_A, SimulationScenario.MIXED, NOW);
            assertThat(reading.pm2_5()).isBetween(0.0, 1000.0);
            assertThat(reading.co2()).isBetween(400.0, 5000.0);
            assertThat(reading.temperature()).isBetween(-10.0, 60.0);
            assertThat(reading.humidity()).isBetween(0.0, 100.0);
        }
    }

    @Test
    void pm1IsStrictlyBelowPm25AndPm10StrictlyAbove() {
        SyntheticTelemetryGeneratorPolicy policy = new SyntheticTelemetryGeneratorPolicy(99L);
        for (int i = 0; i < 50; i++) {
            EnvironmentalTelemetry r = policy.generateOne(DEVICE_A, SimulationScenario.MIXED, NOW);
            assertThat(r.pm1_0()).isLessThan(r.pm2_5());
            assertThat(r.pm2_5()).isLessThan(r.pm10());
        }
    }

    @Test
    void connectivityNetworkAndCountryAreFixed() {
        SyntheticTelemetryGeneratorPolicy policy = new SyntheticTelemetryGeneratorPolicy(11L);
        EnvironmentalTelemetry r = policy.generateOne(DEVICE_A, SimulationScenario.MIXED, NOW);
        assertThat(r.connectivityStatus()).isEqualTo("ONLINE");
        assertThat(r.networkName()).isEqualTo("LOCAL-SIMULATOR");
        assertThat(r.country()).isEqualTo("Peru");
        assertThat(r.recordedAt()).isEqualTo(NOW);
    }

    @Test
    void healthGaugeTracksSeverityMonotonically() {
        assertThat(SyntheticTelemetryGeneratorPolicy.healthFor(EnvironmentalSeverity.RECOMMENDED))
                .isGreaterThan(SyntheticTelemetryGeneratorPolicy.healthFor(EnvironmentalSeverity.ALERT));
        assertThat(SyntheticTelemetryGeneratorPolicy.healthFor(EnvironmentalSeverity.ALERT))
                .isGreaterThan(SyntheticTelemetryGeneratorPolicy.healthFor(EnvironmentalSeverity.HIGH));
    }

    @Test
    void mixedScenarioWeightSumsToOne() {
        SimulationScenario s = SimulationScenario.MIXED;
        assertThat(s.recommendedShare() + s.alertShare() + s.highShare()).isEqualTo(1.0);
    }

    @RepeatedTest(10)
    void zeroSeedDoesNotExplode() {
        EnvironmentalTelemetry r = new SyntheticTelemetryGeneratorPolicy(0L)
                .generateOne(DEVICE_A, SimulationScenario.RECOMMENDED_ONLY, NOW);
        // RECOMMENDED_ONLY has no events and a tight seasonal envelope around 8 ± 3 µg/m³,
        // so the reading must always stay well below the ALERT boundary (25 µg/m³).
        assertThat(r.pm2_5()).isBetween(0.0, 25.0);
    }

    @Test
    void batchMatchesTheNumberOfDeviceIds() {
        SyntheticTelemetryGeneratorPolicy policy = new SyntheticTelemetryGeneratorPolicy(13L);
        List<EnvironmentalTelemetry> readings = policy.generate(
                List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()),
                SimulationScenario.ALERT_ONLY,
                NOW);
        assertThat(readings).hasSize(3);
    }

    @Test
    void nullDeviceIdIsRejected() {
        SyntheticTelemetryGeneratorPolicy policy = new SyntheticTelemetryGeneratorPolicy(1L);
        assertThatThrownBy(() -> policy.generateOne(null, SimulationScenario.MIXED, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- AR(1) realism guarantees ----------------------------------------------

    @Test
    void consecutiveReadingsChangeSmoothlyNoTenToHundredJumps() {
        // The original bug: PM2.5 went from 10 to 100 between two cycles. The AR(1) term
        // φ·(X_{t-1} − μ_{t-1}) bounds the per-step change. We sample 20 cycles and check
        // that no adjacent pair ever moves more than what a real indoor sensor could do
        // in 15 s. A jump of 30 µg/m³ in one cycle is wildly outside indoor physics; the
        // assertion is intentionally generous so a rare event-spike decay still passes.
        SyntheticTelemetryGeneratorPolicy policy = new SyntheticTelemetryGeneratorPolicy(2024L);
        double prev = policy.generateOne(DEVICE_A, SimulationScenario.MIXED, NOW).pm2_5();
        for (int i = 1; i <= 20; i++) {
            double current = policy.generateOne(
                    DEVICE_A, SimulationScenario.MIXED, NOW.plusSeconds(15L * i)).pm2_5();
            assertThat(Math.abs(current - prev))
                    .as("cycle %d: jump from %.2f to %.2f µg/m³ is physically impossible", i, prev, current)
                    .isLessThan(30.0);
            prev = current;
        }
    }

    @Test
    void allMetricsAreIndividuallySmooth() {
        // CO₂ can spike harder than PM2.5 when occupancy jumps, but still no 600 → 1500
        // step in 15 s. Temperature and humidity are even slower — at most a few tenths.
        SyntheticTelemetryGeneratorPolicy policy = new SyntheticTelemetryGeneratorPolicy(2025L);
        EnvironmentalTelemetry first = policy.generateOne(DEVICE_A, SimulationScenario.MIXED, NOW);
        for (int i = 1; i <= 10; i++) {
            EnvironmentalTelemetry next = policy.generateOne(
                    DEVICE_A, SimulationScenario.MIXED, NOW.plusSeconds(15L * i));
            assertThat(Math.abs(next.co2() - first.co2())).isLessThan(400.0);
            assertThat(Math.abs(next.temperature() - first.temperature())).isLessThan(2.0);
            assertThat(Math.abs(next.humidity() - first.humidity())).isLessThan(5.0);
            first = next;
        }
    }

    @Test
    void devicesDriftIndependently() {
        // Two devices with the same seed should not produce identical sequences once the
        // AR(1) state diverges — the per-device phase offset and state map guarantee it.
        SyntheticTelemetryGeneratorPolicy policy = new SyntheticTelemetryGeneratorPolicy(7L);
        double a = policy.generateOne(DEVICE_A, SimulationScenario.MIXED, NOW).pm2_5();
        double b = policy.generateOne(DEVICE_B, SimulationScenario.MIXED, NOW).pm2_5();
        // Different devices start at different seasonal phases, so their first readings
        // cannot both equal the exact same value.
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void scenarioSwitchMovesStateTowardNewSeasonalMean() {
        // MIXED oscillates around PM2.5 ≈ 12; HIGH_ONLY around PM2.5 ≈ 200. After the
        // switch the device must re-seed at the new baseline — no scenario change leaves
        // the previous scenario's mean trailing forever.
        SyntheticTelemetryGeneratorPolicy policy = new SyntheticTelemetryGeneratorPolicy(31L);
        for (int i = 0; i < 40; i++) {
            policy.generateOne(DEVICE_A, SimulationScenario.MIXED, NOW.plusSeconds(15L * i));
        }
        EnvironmentalTelemetry switched = policy.generateOne(
                DEVICE_A, SimulationScenario.HIGH_ONLY, NOW.plusSeconds(15L * 40));
        assertThat(switched.pm2_5()).isGreaterThan(50.0);
    }

    @Test
    void seasonalMeanIsReachableOverManyCycles() {
        // Period is 240 cycles (1 h at 15 s cadence). The seasonal swing should be visible
        // if we sample across half a period: peak and trough of the sine should differ by
        // roughly 2·A on PM2.5. We check that the maximum minus the minimum over 120
        // cycles is at least 4 µg/m³ — i.e. the sine is actually doing work.
        SyntheticTelemetryGeneratorPolicy policy = new SyntheticTelemetryGeneratorPolicy(101L);
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < 120; i++) {
            double pm25 = policy.generateOne(
                    DEVICE_A, SimulationScenario.MIXED, NOW.plusSeconds(15L * i)).pm2_5();
            min = Math.min(min, pm25);
            max = Math.max(max, pm25);
        }
        assertThat(max - min).as("seasonal swing over half a period").isGreaterThan(4.0);
    }
}
