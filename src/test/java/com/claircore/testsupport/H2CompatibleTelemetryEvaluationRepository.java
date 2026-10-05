package com.claircore.testsupport;

import com.claircore.evaluation.domain.model.aggregates.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.valueobjects.DeviceReading;
import com.claircore.evaluation.domain.model.valueobjects.HourlyDeviceAverage;
import com.claircore.evaluation.domain.repositories.TelemetryEvaluationRepository;
import com.claircore.evaluation.infrastructure.persistence.jpa.adapters.TelemetryEvaluationRepositoryImpl;
import com.claircore.evaluation.infrastructure.persistence.jpa.assemblers.TelemetryEvaluationPersistenceAssembler;
import com.claircore.evaluation.infrastructure.persistence.jpa.repositories.TelemetryEvaluationPersistenceRepository;
import com.claircore.shared.domain.model.PageResult;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The production upsert uses PostgreSQL {@code ON CONFLICT}. HTTP suites run on H2, so duplicates
 * are resolved with a find-then-save instead.
 */
public final class H2CompatibleTelemetryEvaluationRepository implements TelemetryEvaluationRepository {

    private final TelemetryEvaluationRepositoryImpl delegate;
    private final TelemetryEvaluationPersistenceRepository jpa;

    public H2CompatibleTelemetryEvaluationRepository(
            TelemetryEvaluationRepositoryImpl delegate,
            TelemetryEvaluationPersistenceRepository jpa) {
        this.delegate = delegate;
        this.jpa = jpa;
    }

    @Override
    public TelemetryEvaluation save(TelemetryEvaluation evaluation) {
        return delegate.save(evaluation);
    }

    @Override
    public StoredReading saveIfAbsent(TelemetryEvaluation evaluation) {
        return jpa.findByDeviceIdAndReadingId(evaluation.getDeviceId(), evaluation.getReadingId())
                .map(existing -> new StoredReading(
                        TelemetryEvaluationPersistenceAssembler.toDomainFromPersistence(existing), false))
                .orElseGet(() -> new StoredReading(delegate.save(evaluation), true));
    }

    @Override
    public PageResult<TelemetryEvaluation> findByDeviceId(UUID deviceId, int page, int size) {
        return delegate.findByDeviceId(deviceId, page, size);
    }

    @Override
    public PageResult<TelemetryEvaluation> findByDeviceIdSince(UUID deviceId, Instant since, int page, int size) {
        return delegate.findByDeviceIdSince(deviceId, since, page, size);
    }

    @Override
    public Optional<TelemetryEvaluation> findLatestByDeviceId(UUID deviceId) {
        return delegate.findLatestByDeviceId(deviceId);
    }

    @Override
    public Optional<TelemetryEvaluation> findLatestByDeviceIdSince(UUID deviceId, Instant since) {
        return delegate.findLatestByDeviceIdSince(deviceId, since);
    }

    @Override
    public void markAlertsEvaluated(UUID deviceId, UUID readingId, Instant at) {
        delegate.markAlertsEvaluated(deviceId, readingId, at);
    }

    @Override
    public List<TelemetryEvaluation> findAlertsPending(Instant createdBefore, int limit) {
        return delegate.findAlertsPending(createdBefore, limit);
    }

    @Override
    public List<HourlyDeviceAverage> findHourlyAveragesBetween(Instant start, Instant end) {
        return delegate.findHourlyAveragesBetween(start, end);
    }

    @Override
    public List<DeviceReading> findReadingsBetween(Instant start, Instant end) {
        return delegate.findReadingsBetween(start, end);
    }
}
