package com.claircore.alerting.infrastructure.persistence.jpa.adapters;

import com.claircore.alerting.domain.model.aggregates.Alert;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.domain.model.valueobjects.DailyAlertCount;
import com.claircore.alerting.domain.model.valueobjects.MetricType;
import com.claircore.alerting.domain.repositories.AlertRepository;
import com.claircore.alerting.infrastructure.persistence.jpa.assemblers.AlertPersistenceAssembler;
import com.claircore.alerting.infrastructure.persistence.jpa.entities.AlertPersistenceEntity;
import com.claircore.alerting.infrastructure.persistence.jpa.entities.AlertTransitionCounterPersistenceEntity;
import com.claircore.alerting.infrastructure.persistence.jpa.repositories.AlertPersistenceRepository;
import com.claircore.alerting.infrastructure.persistence.jpa.repositories.AlertTransitionCounterPersistenceRepository;
import com.claircore.shared.domain.model.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AlertRepositoryImpl implements AlertRepository {

    private final AlertPersistenceRepository alertPersistenceRepository;
    private final AlertTransitionCounterPersistenceRepository counterPersistenceRepository;

    public AlertRepositoryImpl(AlertPersistenceRepository alertPersistenceRepository,
                               AlertTransitionCounterPersistenceRepository counterPersistenceRepository) {
        this.alertPersistenceRepository = alertPersistenceRepository;
        this.counterPersistenceRepository = counterPersistenceRepository;
    }

    @Override
    public Alert save(Alert alert) {
        var saved = alertPersistenceRepository.save(AlertPersistenceAssembler.toPersistenceFromDomain(alert));
        return AlertPersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public Optional<Alert> findById(UUID id) {
        return alertPersistenceRepository.findById(id).map(AlertPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public PageResult<Alert> findByDeviceId(UUID deviceId, int page, int size) {
        return toPageResult(
                alertPersistenceRepository.findByDeviceIdOrderByOccurredAtDesc(deviceId, PageRequest.of(page, size)),
                page, size);
    }

    @Override
    public PageResult<Alert> findBySpaceId(UUID spaceId, int page, int size) {
        return toPageResult(
                alertPersistenceRepository.findBySpaceIdOrderByOccurredAtDesc(spaceId, PageRequest.of(page, size)),
                page, size);
    }

    @Override
    public PageResult<Alert> findByDeviceIdIn(Collection<UUID> deviceIds, int page, int size) {
        return toPageResult(
                alertPersistenceRepository.findByDeviceIdInOrderByOccurredAtDesc(deviceIds, PageRequest.of(page, size)),
                page, size);
    }

    @Override
    public PageResult<Alert> findByDeviceIdAndStatusIn(UUID deviceId, Collection<AlertStatus> statuses, int page, int size) {
        return toPageResult(
                alertPersistenceRepository.findByDeviceIdAndStatusInOrderByOccurredAtDesc(
                        deviceId, statuses, PageRequest.of(page, size)),
                page, size);
    }

    @Override
    public PageResult<Alert> findBySpaceIdAndStatusIn(UUID spaceId, Collection<AlertStatus> statuses, int page, int size) {
        return toPageResult(
                alertPersistenceRepository.findBySpaceIdAndStatusInOrderByOccurredAtDesc(
                        spaceId, statuses, PageRequest.of(page, size)),
                page, size);
    }

    @Override
    public PageResult<Alert> findByDeviceIdInAndStatusIn(Collection<UUID> deviceIds, Collection<AlertStatus> statuses, int page, int size) {
        return toPageResult(
                alertPersistenceRepository.findByDeviceIdInAndStatusInOrderByOccurredAtDesc(
                        deviceIds, statuses, PageRequest.of(page, size)),
                page, size);
    }

    @Override
    public List<Alert> findByDeviceIdAndStatus(UUID deviceId, AlertStatus status) {
        return toDomain(alertPersistenceRepository.findByDeviceIdAndStatus(deviceId, status));
    }

    @Override
    public Optional<Alert> findFirstByDeviceIdAndMetricAndStatusIn(UUID deviceId, MetricType metric, Collection<AlertStatus> statuses) {
        return alertPersistenceRepository.findFirstByDeviceIdAndMetricAndStatusIn(deviceId, metric, statuses)
                .map(AlertPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<Alert> findByIdForAcknowledgement(UUID alertId) {
        return alertPersistenceRepository.findByIdForAcknowledgement(alertId)
                .map(AlertPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<Alert> findPendingForEdge(Collection<AlertStatus> statuses, Long afterSequence, int limit) {
        return toDomain(alertPersistenceRepository.findPendingForEdge(statuses, afterSequence, PageRequest.of(0, limit)));
    }

    /** The counter row is locked for the transaction, so concurrent transitions get distinct numbers. */
    @Override
    public long nextTransitionSequence() {
        var counter = counterPersistenceRepository.lockById(AlertTransitionCounterPersistenceEntity.SINGLETON_ID)
                .orElseGet(() -> counterPersistenceRepository.saveAndFlush(new AlertTransitionCounterPersistenceEntity(0L)));
        counter.setValue(counter.getValue() + 1);
        counterPersistenceRepository.saveAndFlush(counter);
        return counter.getValue();
    }

    @Override
    public List<DailyAlertCount> countAlertsPerDayBySpaceId(UUID spaceId, Instant since) {
        return toDailyAlertCounts(alertPersistenceRepository.countAlertsPerDay(spaceId, since));
    }

    @Override
    public List<DailyAlertCount> countAlertsPerDayByDeviceIds(Collection<UUID> deviceIds, Instant since) {
        return toDailyAlertCounts(alertPersistenceRepository.countAlertsPerDayByDeviceIds(deviceIds, since));
    }

    private static PageResult<Alert> toPageResult(Page<AlertPersistenceEntity> page, int pageNumber, int size) {
        return new PageResult<>(toDomain(page.getContent()), pageNumber, size, page.getTotalElements());
    }

    private static List<Alert> toDomain(List<AlertPersistenceEntity> entities) {
        return entities.stream().map(AlertPersistenceAssembler::toDomainFromPersistence).toList();
    }

    private static List<DailyAlertCount> toDailyAlertCounts(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new DailyAlertCount((LocalDate) row[0], ((Number) row[1]).longValue()))
                .toList();
    }
}
