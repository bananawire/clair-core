package com.claircore.evaluation.infrastructure.persistence.jpa.adapters;

import com.claircore.evaluation.domain.model.aggregates.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.valueobjects.DeviceId;
import com.claircore.evaluation.domain.model.valueobjects.DeviceReading;
import com.claircore.evaluation.domain.model.valueobjects.HourlyDeviceAverage;
import com.claircore.evaluation.domain.repositories.TelemetryEvaluationRepository;
import com.claircore.evaluation.infrastructure.persistence.jpa.assemblers.TelemetryEvaluationPersistenceAssembler;
import com.claircore.evaluation.infrastructure.persistence.jpa.repositories.TelemetryEvaluationPersistenceRepository;
import com.claircore.shared.domain.model.PageResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TelemetryEvaluationRepositoryImpl implements TelemetryEvaluationRepository {

    /**
     * Unchanged from the facade that used to hold it. Every column named here is a column analytics
     * depends on; the DDL gate is what keeps them in place.
     */
    private static final String HOURLY_AVERAGES_SQL = """
            SELECT device_id,
                   AVG(aq_co2) as avg_co2,
                   AVG(pm_pm2_5) as avg_pm2_5,
                   AVG(aq_temperature) as avg_temperature,
                   AVG(aq_humidity) as avg_humidity
            FROM telemetry_evaluations
            WHERE recorded_at >= ? AND recorded_at < ?
              AND aq_co2 BETWEEN 0 AND 1000000 AND aq_temperature BETWEEN -50 AND 100
              AND aq_humidity BETWEEN 0 AND 100
              AND pm_pm1_0 BETWEEN 0 AND 10000
              AND pm_pm2_5 BETWEEN 0 AND 10000
              AND pm_pm10 BETWEEN 0 AND 10000
            GROUP BY device_id
            """;

    /**
     * Moved here verbatim from the analytics aggregation service, which used to run it against this
     * context's table itself. Reading the columns straight through rather than loading aggregates
     * keeps a full day of telemetry off the heap.
     */
    private static final String READINGS_SQL = """
            SELECT device_id, aq_co2, pm_pm2_5, aq_temperature, aq_humidity, recorded_at
            FROM telemetry_evaluations
            WHERE recorded_at >= ? AND recorded_at < ?
              AND aq_co2 BETWEEN 0 AND 1000000 AND aq_temperature BETWEEN -50 AND 100
              AND aq_humidity BETWEEN 0 AND 100
              AND pm_pm1_0 BETWEEN 0 AND 10000
              AND pm_pm2_5 BETWEEN 0 AND 10000
              AND pm_pm10 BETWEEN 0 AND 10000
            ORDER BY device_id, recorded_at
            """;

    private final TelemetryEvaluationPersistenceRepository telemetryEvaluationPersistenceRepository;
    private final JdbcTemplate jdbcTemplate;

    public TelemetryEvaluationRepositoryImpl(
            TelemetryEvaluationPersistenceRepository telemetryEvaluationPersistenceRepository,
            JdbcTemplate jdbcTemplate) {
        this.telemetryEvaluationPersistenceRepository = telemetryEvaluationPersistenceRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public TelemetryEvaluation save(TelemetryEvaluation evaluation) {
        var saved = telemetryEvaluationPersistenceRepository.save(
                TelemetryEvaluationPersistenceAssembler.toPersistenceFromDomain(evaluation));
        return TelemetryEvaluationPersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public StoredReading saveIfAbsent(TelemetryEvaluation e) {
        Instant receivedAt = Instant.now();
        int inserted = jdbcTemplate.update("""
                INSERT INTO telemetry_evaluations
                    (id, device_id, reading_id, uptime_seconds, aq_co2, aq_temperature, aq_humidity,
                     pm_pm1_0, pm_pm2_5, pm_pm10, conn_status, conn_network, conn_signal_strength,
                     location_country, health_status, status, recorded_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (device_id, reading_id) DO NOTHING
                """, e.getId(), e.getDeviceId().value(), e.getReadingId(), e.getUptime(),
                e.getAirQuality().co2(), e.getAirQuality().temperature(), e.getAirQuality().humidity(),
                e.getParticulateMatter().pm1_0(), e.getParticulateMatter().pm2_5(), e.getParticulateMatter().pm10(),
                e.getConnectivity().status(), e.getConnectivity().network(), e.getConnectivity().signalStrength(),
                e.getLocation().country(), e.getHealthStatus(), e.getStatus(),
                Timestamp.from(e.getRecordedAt()), Timestamp.from(receivedAt), Timestamp.from(receivedAt));
        var stored = telemetryEvaluationPersistenceRepository
                .findByDeviceIdAndReadingId(e.getDeviceId(), e.getReadingId()).orElseThrow();
        return new StoredReading(TelemetryEvaluationPersistenceAssembler.toDomainFromPersistence(stored), inserted == 1);
    }

    @Override
    public PageResult<TelemetryEvaluation> findByDeviceId(UUID deviceId, int page, int size) {
        var found = telemetryEvaluationPersistenceRepository
                .findByDeviceIdOrderByRecordedAtDesc(new DeviceId(deviceId), PageRequest.of(page, size));
        return new PageResult<>(
                found.getContent().stream().map(TelemetryEvaluationPersistenceAssembler::toDomainFromPersistence).toList(),
                page,
                size,
                found.getTotalElements());
    }

    @Override
    public PageResult<TelemetryEvaluation> findByDeviceIdSince(UUID deviceId, Instant since, int page, int size) {
        var found = telemetryEvaluationPersistenceRepository
                .findByDeviceIdAndRecordedAtGreaterThanEqualOrderByRecordedAtDesc(
                        new DeviceId(deviceId), since, PageRequest.of(page, size));
        return new PageResult<>(
                found.getContent().stream().map(TelemetryEvaluationPersistenceAssembler::toDomainFromPersistence).toList(),
                page, size, found.getTotalElements());
    }

    @Override
    public Optional<TelemetryEvaluation> findLatestByDeviceIdSince(UUID deviceId, Instant since) {
        return telemetryEvaluationPersistenceRepository
                .findFirstByDeviceIdAndRecordedAtGreaterThanEqualOrderByRecordedAtDesc(new DeviceId(deviceId), since)
                .map(TelemetryEvaluationPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<TelemetryEvaluation> findLatestByDeviceId(UUID deviceId) {
        return telemetryEvaluationPersistenceRepository.findFirstByDeviceIdOrderByRecordedAtDesc(new DeviceId(deviceId))
                .map(TelemetryEvaluationPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public void markAlertsEvaluated(UUID deviceId, UUID readingId, Instant at) {
        telemetryEvaluationPersistenceRepository.markAlertsEvaluated(new DeviceId(deviceId), readingId, at);
    }

    @Override
    public List<TelemetryEvaluation> findAlertsPending(Instant createdBefore, int limit) {
        return telemetryEvaluationPersistenceRepository.findAlertsPending(createdBefore, PageRequest.of(0, limit))
                .stream().map(TelemetryEvaluationPersistenceAssembler::toDomainFromPersistence).toList();
    }

    @Override
    public List<HourlyDeviceAverage> findHourlyAveragesBetween(Instant start, Instant end) {
        return jdbcTemplate.query(
                HOURLY_AVERAGES_SQL,
                (rs, rowNum) -> new HourlyDeviceAverage(
                        toUuid(rs.getObject("device_id")),
                        rs.getDouble("avg_co2"),
                        rs.getDouble("avg_pm2_5"),
                        rs.getDouble("avg_temperature"),
                        rs.getDouble("avg_humidity")),
                Timestamp.from(start),
                Timestamp.from(end));
    }

    @Override
    public List<DeviceReading> findReadingsBetween(Instant start, Instant end) {
        return jdbcTemplate.query(
                READINGS_SQL,
                (rs, rowNum) -> new DeviceReading(
                        toUuid(rs.getObject("device_id")),
                        rs.getDouble("aq_co2"),
                        rs.getDouble("pm_pm2_5"),
                        rs.getDouble("aq_temperature"),
                        rs.getDouble("aq_humidity"),
                        rs.getTimestamp("recorded_at").toInstant()),
                Timestamp.from(start),
                Timestamp.from(end));
    }

    /** Postgres hands back a UUID, H2 a String; the caller used to do this itself. */
    private static UUID toUuid(Object value) {
        return value instanceof UUID uuid ? uuid : UUID.fromString(value.toString());
    }
}
