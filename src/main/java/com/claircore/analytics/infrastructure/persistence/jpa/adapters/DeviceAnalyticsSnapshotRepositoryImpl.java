package com.claircore.analytics.infrastructure.persistence.jpa.adapters;

import com.claircore.analytics.domain.model.aggregates.DeviceAnalyticsSnapshot;
import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.domain.model.valueobjects.MetricAverages;
import com.claircore.analytics.domain.repositories.DeviceAnalyticsSnapshotRepository;
import com.claircore.analytics.infrastructure.persistence.jpa.assemblers.DeviceAnalyticsSnapshotPersistenceAssembler;
import com.claircore.analytics.infrastructure.persistence.jpa.repositories.DeviceAnalyticsSnapshotPersistenceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DeviceAnalyticsSnapshotRepositoryImpl implements DeviceAnalyticsSnapshotRepository {

    private final DeviceAnalyticsSnapshotPersistenceRepository snapshotPersistenceRepository;

    public DeviceAnalyticsSnapshotRepositoryImpl(
            DeviceAnalyticsSnapshotPersistenceRepository snapshotPersistenceRepository) {
        this.snapshotPersistenceRepository = snapshotPersistenceRepository;
    }

    @Override
    public DeviceAnalyticsSnapshot save(DeviceAnalyticsSnapshot snapshot) {
        var entity = DeviceAnalyticsSnapshotPersistenceAssembler.toPersistenceFromDomain(snapshot);
        var existing = snapshotPersistenceRepository.findByDeviceIdAndTimeWindowStartAndTimeWindowEnd(
                snapshot.getDeviceId(), snapshot.getTimeWindowStart(), snapshot.getTimeWindowEnd());
        if (!existing.isEmpty()) {
            entity.setId(existing.getFirst().getId());
            entity.setCreatedAt(existing.getFirst().getCreatedAt());
            // Older versions could append a bucket again on retry. Consolidate derived rows on rebuild.
            snapshotPersistenceRepository.deleteAll(existing.subList(1, existing.size()));
        }
        var saved = snapshotPersistenceRepository.save(entity);
        return DeviceAnalyticsSnapshotPersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public List<DeviceAnalyticsSnapshot> findByDeviceIdAndWindowStartBetween(
            UUID deviceId, Instant start, Instant end, Integer limit) {
        Pageable pageable = limit != null ? PageRequest.of(0, limit) : Pageable.unpaged();
        return snapshotPersistenceRepository
                .findByDeviceIdAndTimeWindowStartGreaterThanEqualAndTimeWindowStartLessThanOrderByTimeWindowStartAsc(
                        new DeviceId(deviceId), start, end, pageable)
                .stream()
                .map(DeviceAnalyticsSnapshotPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public Optional<DeviceAnalyticsSnapshot> findLatestByDeviceId(UUID deviceId) {
        return snapshotPersistenceRepository.findFirstByDeviceIdOrderByTimeWindowEndDesc(new DeviceId(deviceId))
                .map(DeviceAnalyticsSnapshotPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<DeviceAnalyticsSnapshot> findLatestByDeviceIds(List<UUID> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) return List.of();
        return snapshotPersistenceRepository
                .findLatestByDeviceIds(deviceIds.stream().map(DeviceId::new).toList())
                .stream()
                .map(DeviceAnalyticsSnapshotPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public Optional<MetricAverages> findAveragesByDeviceIdAndWindow(UUID deviceId, Instant start, Instant end) {
        var averages = snapshotPersistenceRepository
                .findAveragesByDeviceIdAndWindow(new DeviceId(deviceId), start, end);
        if (averages == null || averages.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new MetricAverages(
                averages.co2(), averages.pm2_5(), averages.temperature(), averages.humidity()));
    }
}
