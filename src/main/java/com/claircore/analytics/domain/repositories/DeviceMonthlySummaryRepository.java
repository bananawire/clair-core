package com.claircore.analytics.domain.repositories;

import com.claircore.analytics.domain.model.aggregates.DeviceMonthlySummary;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** Port for monthly summary storage. Domain types only; {@code month} is the first of the month. */
public interface DeviceMonthlySummaryRepository {

    DeviceMonthlySummary save(DeviceMonthlySummary summary);

    Optional<DeviceMonthlySummary> findByDeviceIdAndMonth(UUID deviceId, LocalDate month);

    boolean existsByDeviceIdAndMonth(UUID deviceId, LocalDate month);
}
