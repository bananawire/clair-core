package com.claircore.analytics.infrastructure.persistence.jpa.repositories;

import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.infrastructure.persistence.jpa.entities.DeviceDailySummaryPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceDailySummaryPersistenceRepository
        extends JpaRepository<DeviceDailySummaryPersistenceEntity, UUID> {

    Optional<DeviceDailySummaryPersistenceEntity> findByDeviceIdAndSummaryDate(DeviceId deviceId, LocalDate summaryDate);

    Optional<DeviceDailySummaryPersistenceEntity> findFirstByDeviceIdOrderBySummaryDateDesc(DeviceId deviceId);

    boolean existsByDeviceIdAndSummaryDate(DeviceId deviceId, LocalDate summaryDate);

    List<DeviceDailySummaryPersistenceEntity>
    findBySummaryDateBetweenOrderByDeviceIdAscSummaryDateAsc(LocalDate start, LocalDate end);
}
