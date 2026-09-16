package com.claircore.analytics.infrastructure.persistence.jpa.adapters;

import com.claircore.analytics.domain.model.aggregates.DeviceMonthlySummary;
import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.domain.repositories.DeviceMonthlySummaryRepository;
import com.claircore.analytics.infrastructure.persistence.jpa.assemblers.DeviceMonthlySummaryPersistenceAssembler;
import com.claircore.analytics.infrastructure.persistence.jpa.repositories.DeviceMonthlySummaryPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DeviceMonthlySummaryRepositoryImpl implements DeviceMonthlySummaryRepository {

    private final DeviceMonthlySummaryPersistenceRepository monthlySummaryPersistenceRepository;

    public DeviceMonthlySummaryRepositoryImpl(
            DeviceMonthlySummaryPersistenceRepository monthlySummaryPersistenceRepository) {
        this.monthlySummaryPersistenceRepository = monthlySummaryPersistenceRepository;
    }

    @Override
    public DeviceMonthlySummary save(DeviceMonthlySummary summary) {
        var entity = DeviceMonthlySummaryPersistenceAssembler.toPersistenceFromDomain(summary);
        monthlySummaryPersistenceRepository.findByDeviceIdAndSummaryMonth(summary.getDeviceId(), summary.getSummaryMonth())
                .ifPresent(existing -> {
                    entity.setId(existing.getId());
                    entity.setCreatedAt(existing.getCreatedAt());
                });
        var saved = monthlySummaryPersistenceRepository.save(entity);
        return DeviceMonthlySummaryPersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public Optional<DeviceMonthlySummary> findByDeviceIdAndMonth(UUID deviceId, LocalDate month) {
        return monthlySummaryPersistenceRepository
                .findByDeviceIdAndSummaryMonth(new DeviceId(deviceId), month.withDayOfMonth(1))
                .map(DeviceMonthlySummaryPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public boolean existsByDeviceIdAndMonth(UUID deviceId, LocalDate month) {
        return monthlySummaryPersistenceRepository
                .existsByDeviceIdAndSummaryMonth(new DeviceId(deviceId), month.withDayOfMonth(1));
    }
}
