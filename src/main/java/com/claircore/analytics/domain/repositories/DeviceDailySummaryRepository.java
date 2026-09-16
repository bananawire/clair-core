package com.claircore.analytics.domain.repositories;

import com.claircore.analytics.domain.model.aggregates.DeviceDailySummary;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port for daily summary storage. Domain types only. */
public interface DeviceDailySummaryRepository {

    DeviceDailySummary save(DeviceDailySummary summary);

    Optional<DeviceDailySummary> findByDeviceIdAndDate(UUID deviceId, LocalDate date);

    /** The most recent completed day this device has a summary for. */
    Optional<DeviceDailySummary> findLatestByDeviceId(UUID deviceId);

    boolean existsByDeviceIdAndDate(UUID deviceId, LocalDate date);

    /** Every device's summaries across [start, end] inclusive, device then date ascending. */
    List<DeviceDailySummary> findAllByDateBetween(LocalDate start, LocalDate end);
}
