package com.claircore.alerting.domain.repositories;

import com.claircore.alerting.domain.model.aggregates.Alert;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.domain.model.valueobjects.DailyAlertCount;
import com.claircore.alerting.domain.model.valueobjects.MetricType;
import com.claircore.shared.domain.model.PageResult;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for alert storage. Domain types only.
 *
 * <p>Every paged method orders by {@code occurredAt} descending; the column name stays in the
 * adapter. Nothing here reaches into another context's tables: the hardware id an edge caller needs
 * is resolved through the device facade, not by joining {@code devices}.
 */
public interface AlertRepository {

    Alert save(Alert alert);

    Optional<Alert> findById(UUID id);

    PageResult<Alert> findByDeviceId(UUID deviceId, int page, int size);

    PageResult<Alert> findBySpaceId(UUID spaceId, int page, int size);

    PageResult<Alert> findByDeviceIdIn(Collection<UUID> deviceIds, int page, int size);

    PageResult<Alert> findByDeviceIdAndStatusIn(UUID deviceId, Collection<AlertStatus> statuses, int page, int size);

    PageResult<Alert> findBySpaceIdAndStatusIn(UUID spaceId, Collection<AlertStatus> statuses, int page, int size);

    PageResult<Alert> findByDeviceIdInAndStatusIn(Collection<UUID> deviceIds, Collection<AlertStatus> statuses, int page, int size);

    List<Alert> findByDeviceIdAndStatus(UUID deviceId, AlertStatus status);

    Optional<Alert> findFirstByDeviceIdAndMetricAndStatusIn(UUID deviceId, MetricType metric, Collection<AlertStatus> statuses);

    /** Locks the row for the duration of the transaction so a concurrent acknowledgement cannot interleave. */
    Optional<Alert> findByIdForAcknowledgement(UUID alertId);

    /**
     * Transitions the edge has not confirmed yet, in sequence order: every alert whose sequence is
     * above {@code afterSequence} (null means from the start) and above its own edge receipt.
     */
    List<Alert> findPendingForEdge(Collection<AlertStatus> statuses, Long afterSequence, int limit);
    /** Hands out the next transition sequence; strictly increasing across every caller. */
    long nextTransitionSequence();

    List<DailyAlertCount> countAlertsPerDayBySpaceId(UUID spaceId, Instant since);

    List<DailyAlertCount> countAlertsPerDayByDeviceIds(Collection<UUID> deviceIds, Instant since);
}
