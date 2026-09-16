package com.claircore.analytics.domain.repositories;

import com.claircore.analytics.domain.model.aggregates.DeviceAnalyticsSnapshot;
import com.claircore.analytics.domain.model.valueobjects.MetricAverages;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port for hourly analytics snapshot storage. Domain types only. */
public interface DeviceAnalyticsSnapshotRepository {

    DeviceAnalyticsSnapshot save(DeviceAnalyticsSnapshot snapshot);

    /**
     * Snapshots whose window opens in [start, end), oldest first. A null {@code limit} means every
     * matching snapshot.
     */
    List<DeviceAnalyticsSnapshot> findByDeviceIdAndWindowStartBetween(
            UUID deviceId, Instant start, Instant end, Integer limit);

    Optional<DeviceAnalyticsSnapshot> findLatestByDeviceId(UUID deviceId);

    /** The most recent snapshot of each given device, in one round trip. */
    List<DeviceAnalyticsSnapshot> findLatestByDeviceIds(List<UUID> deviceIds);

    /** Averages across every snapshot in [start, end); empty when the window holds none. */
    Optional<MetricAverages> findAveragesByDeviceIdAndWindow(UUID deviceId, Instant start, Instant end);
}
