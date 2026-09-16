package com.claircore.evaluation.infrastructure.persistence.jpa.adapters;

import com.claircore.evaluation.domain.model.aggregates.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.valueobjects.AirQuality;
import com.claircore.evaluation.domain.model.valueobjects.Connectivity;
import com.claircore.evaluation.domain.model.valueobjects.DeviceId;
import com.claircore.evaluation.domain.model.valueobjects.Location;
import com.claircore.evaluation.domain.model.valueobjects.ParticulateMatter;
import com.claircore.evaluation.domain.repositories.TelemetryEvaluationRepository;
import com.claircore.shared.infrastructure.config.JpaAuditingConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the port through its adapter, and guards the column names analytics reads with raw SQL:
 * a rename here is a broken report, and no other test would notice.
 */
@DataJpaTest
@Import({JpaAuditingConfiguration.class, TelemetryEvaluationRepositoryImpl.class})
class TelemetryEvaluationRepositoryImplTest {

    @Autowired
    private TelemetryEvaluationRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestEntityManager entityManager;

    private static final Instant RECORDED_AT = Instant.parse("2026-01-01T00:00:00Z");

    /**
     * A day either side of the readings. The query binds a java.sql.Timestamp, so the driver reads
     * the bounds in the database session's time zone — unchanged from the facade that used to hold
     * this SQL, but it means a window measured in minutes would make this test depend on where it
     * runs. A day of slack is wider than any offset; the reading it must exclude sits days away.
     */
    private static final Instant WINDOW_START = RECORDED_AT.minus(java.time.Duration.ofDays(1));
    private static final Instant WINDOW_END = RECORDED_AT.plus(java.time.Duration.ofDays(1));
    private static final Instant OUTSIDE_WINDOW = RECORDED_AT.plus(java.time.Duration.ofDays(5));

    @Test
    void savesWithTheIdentityTheAggregateAssignedAndFillsTheAuditTimestamps() {
        var reading = reading(UUID.randomUUID(), RECORDED_AT, 400.0, 10);

        var saved = repository.save(reading);

        assertThat(saved.getId()).isEqualTo(reading.getId());
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void pagesByDeviceMostRecentlyRecordedFirst() {
        UUID deviceId = UUID.randomUUID();
        repository.save(reading(deviceId, RECORDED_AT, 400.0, 10));
        repository.save(reading(deviceId, RECORDED_AT.plusSeconds(600), 500.0, 20));
        repository.save(reading(UUID.randomUUID(), RECORDED_AT, 600.0, 30));

        var page = repository.findByDeviceId(deviceId, 0, 10);

        assertThat(page.total()).isEqualTo(2);
        assertThat(page.items()).hasSize(2);
        assertThat(page.items().get(0).getRecordedAt()).isEqualTo(RECORDED_AT.plusSeconds(600));
        assertThat(page.items().get(1).getRecordedAt()).isEqualTo(RECORDED_AT);
    }

    @Test
    void findsTheLatestReadingForADevice() {
        UUID deviceId = UUID.randomUUID();
        repository.save(reading(deviceId, RECORDED_AT, 400.0, 10));
        repository.save(reading(deviceId, RECORDED_AT.plusSeconds(600), 500.0, 20));

        var latest = repository.findLatestByDeviceId(deviceId);

        assertThat(latest).isPresent();
        assertThat(latest.get().getAirQuality().co2()).isEqualTo(500.0);
    }

    @Test
    void sinceBoundHidesReadingsRecordedBeforeTheCurrentClaim() {
        UUID deviceId = UUID.randomUUID();
        repository.save(reading(deviceId, RECORDED_AT, 400.0, 10));
        repository.save(reading(deviceId, RECORDED_AT.plusSeconds(600), 500.0, 20));
        var claimedAt = RECORDED_AT.plusSeconds(300);

        var page = repository.findByDeviceIdSince(deviceId, claimedAt, 0, 10);
        assertThat(page.total()).isEqualTo(1);
        assertThat(page.items().getFirst().getRecordedAt()).isEqualTo(RECORDED_AT.plusSeconds(600));
        assertThat(repository.findLatestByDeviceIdSince(deviceId, claimedAt))
                .map(r -> r.getAirQuality().co2()).contains(500.0);
        assertThat(repository.findLatestByDeviceIdSince(deviceId, RECORDED_AT.plusSeconds(601))).isEmpty();
    }

    @Test
    void receiptsHideReadingsFromTheAlertCatchUp() {
        UUID deviceId = UUID.randomUUID();
        var done = repository.save(reading(deviceId, RECORDED_AT, 400.0, 10));
        var pending = repository.save(reading(deviceId, RECORDED_AT.plusSeconds(60), 500.0, 20));
        entityManager.flush();
        var future = Instant.now().plusSeconds(60);
        assertThat(repository.findAlertsPending(future, 10)).extracting(TelemetryEvaluation::getId)
                .containsExactly(done.getId(), pending.getId());
        repository.markAlertsEvaluated(deviceId, done.getReadingId(), Instant.now());
        assertThat(repository.findAlertsPending(future, 10)).extracting(TelemetryEvaluation::getId)
                .containsExactly(pending.getId());
        // Readings stored after the cutoff are left to the normal after-commit path.
        assertThat(repository.findAlertsPending(Instant.now().minusSeconds(3600), 10)).isEmpty();
        // Idempotent: a second receipt for the same reading is a no-op.
        repository.markAlertsEvaluated(deviceId, done.getReadingId(), Instant.now());
    }

    @Test
    void averagesPerDeviceOverTheWindowAndExcludesReadingsOutsideIt() {
        UUID deviceId = UUID.randomUUID();
        repository.save(reading(deviceId, RECORDED_AT, 400.0, 10));
        repository.save(reading(deviceId, RECORDED_AT.plusSeconds(600), 600.0, 20));
        repository.save(reading(deviceId, OUTSIDE_WINDOW, 1000.0, 99));

        entityManager.flush(); // raw SQL shares the transaction but not the persistence context

        var rows = repository.findHourlyAveragesBetween(WINDOW_START, WINDOW_END);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).deviceId()).isEqualTo(deviceId);
        assertThat(rows.get(0).averageCo2()).isEqualTo(500.0);
        assertThat(rows.get(0).averagePm25()).isEqualTo(15.0);
    }

    /**
     * The column list of analytics' DailyReportAggregationService, which reads this table with its
     * own JdbcTemplate. It is a boundary violation that analytics is scheduled to lose, but until
     * then this query has to keep resolving.
     */
    @Test
    void theColumnsAnalyticsReadsDirectlyStillResolve() {
        UUID deviceId = UUID.randomUUID();
        repository.save(reading(deviceId, RECORDED_AT, 400.0, 10));

        entityManager.flush(); // raw SQL shares the transaction but not the persistence context

        var rows = jdbcTemplate.queryForList("""
                SELECT device_id, aq_co2, pm_pm2_5, aq_temperature, aq_humidity, recorded_at
                FROM telemetry_evaluations
                WHERE recorded_at >= ? AND recorded_at < ?
                ORDER BY device_id, recorded_at
                """, Timestamp.from(WINDOW_START), Timestamp.from(WINDOW_END));

        assertThat(rows).hasSize(1);
        assertThat(((Number) rows.get(0).get("aq_co2")).doubleValue()).isEqualTo(400.0);
        assertThat(((Number) rows.get(0).get("pm_pm2_5")).doubleValue()).isEqualTo(10.0);
        assertThat(rows.get(0).get("recorded_at")).isNotNull();
    }

    private TelemetryEvaluation reading(UUID deviceId, Instant recordedAt, double co2, double pm25) {
        return new TelemetryEvaluation(
                new DeviceId(deviceId),
                UUID.randomUUID(),
                3600L,
                new AirQuality(co2, 22.0, 45.0),
                new ParticulateMatter(5.0, pm25, 25.0),
                new Connectivity("ONLINE", "WiFi", -50),
                new Location("Chile"),
                85,
                "STABLE",
                recordedAt);
    }
}
