package com.claircore.localedge.domain.services;

import com.claircore.localedge.domain.model.valueobjects.EnvironmentalSeverity;
import com.claircore.localedge.domain.model.valueobjects.EnvironmentalTelemetry;
import com.claircore.localedge.domain.model.valueobjects.SimulationScenario;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SyntheticTelemetryGeneratorPolicyTest {

    private static final Instant NOW = Instant.parse("2026-05-16T22:30:00Z");

    @Test
    void deterministicSeedProducesTheSameReadingEveryTime() {
        long seed = 42L;
        var a = new SyntheticTelemetryGeneratorPolicy(seed).generateOne(SimulationScenario.MIXED, NOW);
        var b = new SyntheticTelemetryGeneratorPolicy(seed).generateOne(SimulationScenario.MIXED, NOW);

        assertThat(a).isEqualTo(b);
    }

    @Test
    void differentSeedsProduceDifferentReadings() {
        var a = new SyntheticTelemetryGeneratorPolicy(1L).generateOne(SimulationScenario.MIXED, NOW);
        var b = new SyntheticTelemetryGeneratorPolicy(2L).generateOne(SimulationScenario.MIXED, NOW);

        assertThat(a.pm2_5()).isNotEqualTo(b.pm2_5());
    }

    @Test
    void mixedScenarioStaysInsideTheIndoorRange() {
        var policy = new SyntheticTelemetryGeneratorPolicy(7L);
        // PM2.5 / CO₂ / temperature / humidity are bounded above.
        for (int i = 0; i < 200; i++) {
            EnvironmentalTelemetry reading = policy.generateOne(SimulationScenario.MIXED, NOW);
            assertThat(reading.pm2_5()).isBetween(0.0, 1000.0);
            assertThat(reading.pm2_5()).isLessThanOrEqualTo(1000.0);
            assertThat(reading.co2()).isBetween(400.0, 5000.0);
            assertThat(reading.temperature()).isBetween(-10.0, 60.0);
            assertThat(reading.humidity()).isBetween(0.0, 100.0);
        }
    }

    @Test
    void pm1IsStrictlyBelowPm25AndPm10StrictlyAbove() {
        var policy = new SyntheticTelemetryGeneratorPolicy(99L);
        for (int i = 0; i < 50; i++) {
            EnvironmentalTelemetry r = policy.generateOne(SimulationScenario.MIXED, NOW);
            assertThat(r.pm1_0()).isLessThan(r.pm2_5());
            assertThat(r.pm2_5()).isLessThan(r.pm10());
        }
    }

    @Test
    void connectivityNetworkAndCountryAreFixed() {
        var policy = new SyntheticTelemetryGeneratorPolicy(11L);
        EnvironmentalTelemetry r = policy.generateOne(SimulationScenario.MIXED, NOW);
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
        var s = SimulationScenario.MIXED;
        assertThat(s.recommendedShare() + s.alertShare() + s.highShare()).isEqualTo(1.0);
    }

    @RepeatedTest(10)
    void zeroSeedDoesNotExplode() {
        // 0L → SecureRandom path. We just want to prove the policy still returns a valid reading.
        EnvironmentalTelemetry r = new SyntheticTelemetryGeneratorPolicy(0L)
                .generateOne(SimulationScenario.RECOMMENDED_ONLY, NOW);
        assertThat(r.pm2_5()).isBetween(0.0, 25.0);
    }

    @Test
    void batchMatchesTheNumberOfDeviceIds() {
        var policy = new SyntheticTelemetryGeneratorPolicy(13L);
        var deviceIds = java.util.List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        var readings = policy.generate(deviceIds, SimulationScenario.ALERT_ONLY, NOW);
        assertThat(readings).hasSize(3);
    }
}
