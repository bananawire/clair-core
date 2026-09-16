package com.claircore.evaluation.infrastructure.persistence.jpa.assemblers;

import com.claircore.evaluation.domain.model.aggregates.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.valueobjects.AirQuality;
import com.claircore.evaluation.domain.model.valueobjects.Connectivity;
import com.claircore.evaluation.domain.model.valueobjects.DeviceId;
import com.claircore.evaluation.domain.model.valueobjects.Location;
import com.claircore.evaluation.domain.model.valueobjects.ParticulateMatter;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TelemetryEvaluationPersistenceAssemblerTest {

    @Test
    void everyFieldSurvivesARoundTrip() {
        var original = TelemetryEvaluation.reconstitute(
                UUID.randomUUID(),
                new DeviceId(UUID.randomUUID()),
                UUID.fromString("00000000-0000-0000-0000-000000000123"),
                3600L,
                new AirQuality(400.0, 22.0, 45.0),
                new ParticulateMatter(10.0, 15.0, 25.0),
                new Connectivity("ONLINE", "WiFi", -50),
                new Location("Chile"),
                85,
                "STABLE",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:01Z"),
                Instant.parse("2026-01-01T00:00:02Z"));

        var roundTripped = TelemetryEvaluationPersistenceAssembler.toDomainFromPersistence(
                TelemetryEvaluationPersistenceAssembler.toPersistenceFromDomain(original));

        assertThat(roundTripped.getId()).isEqualTo(original.getId());
        assertThat(roundTripped.getDeviceId()).isEqualTo(original.getDeviceId());
        assertThat(roundTripped.getReadingId()).isEqualTo(original.getReadingId());
        assertThat(roundTripped.getUptime()).isEqualTo(3600L);
        assertThat(roundTripped.getAirQuality()).isEqualTo(original.getAirQuality());
        assertThat(roundTripped.getParticulateMatter()).isEqualTo(original.getParticulateMatter());
        assertThat(roundTripped.getConnectivity()).isEqualTo(original.getConnectivity());
        assertThat(roundTripped.getLocation()).isEqualTo(original.getLocation());
        assertThat(roundTripped.getHealthStatus()).isEqualTo(85);
        assertThat(roundTripped.getStatus()).isEqualTo("STABLE");
        assertThat(roundTripped.getRecordedAt()).isEqualTo(original.getRecordedAt());
        assertThat(roundTripped.getCreatedAt()).isEqualTo(original.getCreatedAt());
        assertThat(roundTripped.getUpdatedAt()).isEqualTo(original.getUpdatedAt());
    }

    @Test
    void nullConnectivityNetworkAndSignalStrengthSurvive() {
        var original = new TelemetryEvaluation(
                new DeviceId(UUID.randomUUID()), UUID.fromString("00000000-0000-0000-0000-000000000123"), 1L,
                new AirQuality(400.0, 22.0, 45.0),
                new ParticulateMatter(10.0, 15.0, 25.0),
                new Connectivity("ONLINE", null, null),
                new Location("Chile"), 85, "STABLE", Instant.now());

        var roundTripped = TelemetryEvaluationPersistenceAssembler.toDomainFromPersistence(
                TelemetryEvaluationPersistenceAssembler.toPersistenceFromDomain(original));

        assertThat(roundTripped.getConnectivity().network()).isNull();
        assertThat(roundTripped.getConnectivity().signalStrength()).isNull();
    }

    @Test
    void anUnsavedReadingMapsToAnEntityThatCountsAsNew() {
        var entity = TelemetryEvaluationPersistenceAssembler.toPersistenceFromDomain(
                new TelemetryEvaluation(
                        new DeviceId(UUID.randomUUID()), UUID.fromString("00000000-0000-0000-0000-000000000123"), 1L,
                        new AirQuality(400.0, 22.0, 45.0),
                        new ParticulateMatter(10.0, 15.0, 25.0),
                        new Connectivity("ONLINE", "WiFi", -50),
                        new Location("Chile"), 85, "STABLE", Instant.now()));

        assertThat(entity.isNew()).isTrue();
    }

    @Test
    void assemblerIsNullSafe() {
        assertThat(TelemetryEvaluationPersistenceAssembler.toDomainFromPersistence(null)).isNull();
        assertThat(TelemetryEvaluationPersistenceAssembler.toPersistenceFromDomain(null)).isNull();
    }
}
