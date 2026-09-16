package com.claircore.alerting.infrastructure.persistence.jpa.repositories;

import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.domain.model.valueobjects.MetricType;
import com.claircore.alerting.infrastructure.persistence.jpa.entities.AlertPersistenceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertPersistenceRepository extends JpaRepository<AlertPersistenceEntity, UUID> {

    Page<AlertPersistenceEntity> findByDeviceIdOrderByOccurredAtDesc(UUID deviceId, Pageable pageable);

    Page<AlertPersistenceEntity> findBySpaceIdOrderByOccurredAtDesc(UUID spaceId, Pageable pageable);

    Page<AlertPersistenceEntity> findByDeviceIdInOrderByOccurredAtDesc(Collection<UUID> deviceIds, Pageable pageable);

    Page<AlertPersistenceEntity> findByDeviceIdAndStatusInOrderByOccurredAtDesc(
            UUID deviceId, Collection<AlertStatus> statuses, Pageable pageable);

    Page<AlertPersistenceEntity> findBySpaceIdAndStatusInOrderByOccurredAtDesc(
            UUID spaceId, Collection<AlertStatus> statuses, Pageable pageable);

    Page<AlertPersistenceEntity> findByDeviceIdInAndStatusInOrderByOccurredAtDesc(
            Collection<UUID> deviceIds, Collection<AlertStatus> statuses, Pageable pageable);

    List<AlertPersistenceEntity> findByDeviceIdAndStatus(UUID deviceId, AlertStatus status);

    Optional<AlertPersistenceEntity> findFirstByDeviceIdAndMetricAndStatusIn(
            UUID deviceId, MetricType metric, Collection<AlertStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AlertPersistenceEntity a WHERE a.id = :alertId")
    Optional<AlertPersistenceEntity> findByIdForAcknowledgement(@Param("alertId") UUID alertId);

    // COALESCE avoids a bare "? IS NULL" placeholder: Postgres cannot infer that parameter's type
    // (no typed context to unify with) and rejects the query with "could not determine data type of
    // parameter $1". Comparing against the column itself when :since is null keeps the original
    // "no filter" semantics.
    @Query("""
            SELECT a FROM AlertPersistenceEntity a
            WHERE a.status IN :statuses
              AND a.transitionSequence > COALESCE(:afterSequence, -1L)
              AND (a.edgeReceiptSequence IS NULL OR a.edgeReceiptSequence < a.transitionSequence)
            ORDER BY a.transitionSequence ASC
            """)
    List<AlertPersistenceEntity> findPendingForEdge(
            @Param("statuses") Collection<AlertStatus> statuses,
            @Param("afterSequence") Long afterSequence,
            Pageable pageable
    );

    @Query("SELECT cast(a.occurredAt as java.time.LocalDate), count(a) FROM AlertPersistenceEntity a WHERE a.spaceId = :spaceId AND a.occurredAt >= :since GROUP BY cast(a.occurredAt as java.time.LocalDate) ORDER BY cast(a.occurredAt as java.time.LocalDate)")
    List<Object[]> countAlertsPerDay(@Param("spaceId") UUID spaceId, @Param("since") Instant since);

    @Query("SELECT cast(a.occurredAt as java.time.LocalDate), count(a) FROM AlertPersistenceEntity a WHERE a.deviceId IN :deviceIds AND a.occurredAt >= :since GROUP BY cast(a.occurredAt as java.time.LocalDate) ORDER BY cast(a.occurredAt as java.time.LocalDate)")
    List<Object[]> countAlertsPerDayByDeviceIds(@Param("deviceIds") Collection<UUID> deviceIds, @Param("since") Instant since);
}
