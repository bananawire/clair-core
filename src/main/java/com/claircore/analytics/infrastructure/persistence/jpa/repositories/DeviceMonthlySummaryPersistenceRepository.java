package com.claircore.analytics.infrastructure.persistence.jpa.repositories;

import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.infrastructure.persistence.jpa.entities.DeviceMonthlySummaryPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceMonthlySummaryPersistenceRepository
        extends JpaRepository<DeviceMonthlySummaryPersistenceEntity, UUID> {

    Optional<DeviceMonthlySummaryPersistenceEntity> findByDeviceIdAndSummaryMonth(DeviceId deviceId, LocalDate summaryMonth);

    boolean existsByDeviceIdAndSummaryMonth(DeviceId deviceId, LocalDate summaryMonth);
}
