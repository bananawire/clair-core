package com.claircore.evaluation.infrastructure.persistence.jpa.repositories;

import com.claircore.evaluation.domain.model.valueobjects.DeviceId;
import com.claircore.evaluation.infrastructure.persistence.jpa.entities.TelemetryEvaluationPersistenceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TelemetryEvaluationPersistenceRepository extends JpaRepository<TelemetryEvaluationPersistenceEntity, UUID> {

    Optional<TelemetryEvaluationPersistenceEntity> findByDeviceIdAndReadingId(DeviceId deviceId, UUID readingId);

    Page<TelemetryEvaluationPersistenceEntity> findByDeviceIdOrderByRecordedAtDesc(DeviceId deviceId, Pageable pageable);

    Optional<TelemetryEvaluationPersistenceEntity> findFirstByDeviceIdOrderByRecordedAtDesc(DeviceId deviceId);
    Page<TelemetryEvaluationPersistenceEntity> findByDeviceIdAndRecordedAtGreaterThanEqualOrderByRecordedAtDesc(
            DeviceId deviceId, java.time.Instant since, Pageable pageable);
    Optional<TelemetryEvaluationPersistenceEntity> findFirstByDeviceIdAndRecordedAtGreaterThanEqualOrderByRecordedAtDesc(
            DeviceId deviceId, java.time.Instant since);
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("""
            UPDATE TelemetryEvaluationPersistenceEntity e SET e.alertsEvaluatedAt = :at
            WHERE e.deviceId = :deviceId AND e.readingId = :readingId AND e.alertsEvaluatedAt IS NULL
            """)
    int markAlertsEvaluated(@org.springframework.data.repository.query.Param("deviceId") DeviceId deviceId,
                            @org.springframework.data.repository.query.Param("readingId") UUID readingId,
                            @org.springframework.data.repository.query.Param("at") java.time.Instant at);
    @org.springframework.data.jpa.repository.Query("""
            SELECT e FROM TelemetryEvaluationPersistenceEntity e
            WHERE e.alertsEvaluatedAt IS NULL AND e.createdAt < :createdBefore
            ORDER BY e.recordedAt ASC
            """)
    java.util.List<TelemetryEvaluationPersistenceEntity> findAlertsPending(
            @org.springframework.data.repository.query.Param("createdBefore") java.time.Instant createdBefore, Pageable pageable);
}
