package com.claircore.evaluation.domain.repositories;

import com.claircore.evaluation.domain.model.aggregates.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.valueobjects.DeviceReading;
import com.claircore.evaluation.domain.model.valueobjects.HourlyDeviceAverage;
import com.claircore.shared.domain.model.PageResult;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port for telemetry evaluation storage. Domain types only. */
public interface TelemetryEvaluationRepository {

    TelemetryEvaluation save(TelemetryEvaluation evaluation);

    /** Atomic insert-or-return-existing; a duplicate must not publish another event. */
    StoredReading saveIfAbsent(TelemetryEvaluation evaluation);

    record StoredReading(TelemetryEvaluation reading, boolean inserted) {}


    /** Most recently recorded first. */
    PageResult<TelemetryEvaluation> findByDeviceId(UUID deviceId, int page, int size);

    /** As {@link #findByDeviceId}, restricted to readings recorded at or after {@code since}. */
    PageResult<TelemetryEvaluation> findByDeviceIdSince(UUID deviceId, Instant since, int page, int size);
    Optional<TelemetryEvaluation> findLatestByDeviceId(UUID deviceId);
    Optional<TelemetryEvaluation> findLatestByDeviceIdSince(UUID deviceId, Instant since);
    /** Records that alert evaluation handled this reading; idempotent. */
    void markAlertsEvaluated(UUID deviceId, UUID readingId, Instant at);
    /** Readings whose alert evaluation never completed, oldest measurement first. */
    List<TelemetryEvaluation> findAlertsPending(Instant createdBefore, int limit);

    /** Per-device averages over [start, end), the aggregation analytics reads hourly. */
    List<HourlyDeviceAverage> findHourlyAveragesBetween(Instant start, Instant end);

    /** Every reading in [start, end), device then time ascending, as analytics summarises them. */
    List<DeviceReading> findReadingsBetween(Instant start, Instant end);
}
