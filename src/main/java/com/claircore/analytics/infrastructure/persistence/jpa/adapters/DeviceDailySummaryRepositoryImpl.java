package com.claircore.analytics.infrastructure.persistence.jpa.adapters;

import com.claircore.analytics.domain.model.aggregates.DeviceDailySummary;
import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.domain.repositories.DeviceDailySummaryRepository;
import com.claircore.analytics.infrastructure.persistence.jpa.assemblers.DeviceDailySummaryPersistenceAssembler;
import com.claircore.analytics.infrastructure.persistence.jpa.repositories.DeviceDailySummaryPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DeviceDailySummaryRepositoryImpl implements DeviceDailySummaryRepository {

    private final DeviceDailySummaryPersistenceRepository dailySummaryPersistenceRepository;

    public DeviceDailySummaryRepositoryImpl(
            DeviceDailySummaryPersistenceRepository dailySummaryPersistenceRepository) {
        this.dailySummaryPersistenceRepository = dailySummaryPersistenceRepository;
    }

    @Override
    public DeviceDailySummary save(DeviceDailySummary summary) {
        var entity = DeviceDailySummaryPersistenceAssembler.toPersistenceFromDomain(summary);
        dailySummaryPersistenceRepository.findByDeviceIdAndSummaryDate(summary.getDeviceId(), summary.getSummaryDate())
                .ifPresent(existing -> {
                    entity.setId(existing.getId());
                    entity.setCreatedAt(existing.getCreatedAt());
                });
        var saved = dailySummaryPersistenceRepository.save(entity);
        return DeviceDailySummaryPersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public Optional<DeviceDailySummary> findByDeviceIdAndDate(UUID deviceId, LocalDate date) {
        return dailySummaryPersistenceRepository.findByDeviceIdAndSummaryDate(new DeviceId(deviceId), date)
                .map(DeviceDailySummaryPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<DeviceDailySummary> findLatestByDeviceId(UUID deviceId) {
        return dailySummaryPersistenceRepository.findFirstByDeviceIdOrderBySummaryDateDesc(new DeviceId(deviceId))
                .map(DeviceDailySummaryPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public boolean existsByDeviceIdAndDate(UUID deviceId, LocalDate date) {
        return dailySummaryPersistenceRepository.existsByDeviceIdAndSummaryDate(new DeviceId(deviceId), date);
    }

    @Override
    public List<DeviceDailySummary> findAllByDateBetween(LocalDate start, LocalDate end) {
        return dailySummaryPersistenceRepository
                .findBySummaryDateBetweenOrderByDeviceIdAscSummaryDateAsc(start, end)
                .stream()
                .map(DeviceDailySummaryPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }
}
