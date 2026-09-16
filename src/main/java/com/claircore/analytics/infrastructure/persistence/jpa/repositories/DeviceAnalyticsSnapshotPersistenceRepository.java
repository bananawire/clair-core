package com.claircore.analytics.infrastructure.persistence.jpa.repositories;

import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.infrastructure.persistence.jpa.entities.DeviceAnalyticsSnapshotPersistenceEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceAnalyticsSnapshotPersistenceRepository
        extends JpaRepository<DeviceAnalyticsSnapshotPersistenceEntity, UUID> {

    List<DeviceAnalyticsSnapshotPersistenceEntity> findByDeviceIdAndTimeWindowStartAndTimeWindowEnd(
            DeviceId deviceId, Instant start, Instant end);

    List<DeviceAnalyticsSnapshotPersistenceEntity>
    findByDeviceIdAndTimeWindowStartGreaterThanEqualAndTimeWindowStartLessThanOrderByTimeWindowStartAsc(
            DeviceId deviceId, Instant start, Instant end, Pageable pageable);

    Optional<DeviceAnalyticsSnapshotPersistenceEntity> findFirstByDeviceIdOrderByTimeWindowEndDesc(DeviceId deviceId);

    /**
     * The latest snapshot of each device in one statement. The correlated subquery is what makes it
     * one round trip instead of one per device, which is why this is not a derived query.
     */
    @Query("""
            SELECT s FROM DeviceAnalyticsSnapshotPersistenceEntity s
            WHERE s.deviceId IN :deviceIds
              AND s.timeWindowEnd = (
                  SELECT MAX(s2.timeWindowEnd) FROM DeviceAnalyticsSnapshotPersistenceEntity s2
                  WHERE s2.deviceId = s.deviceId)
            """)
    List<DeviceAnalyticsSnapshotPersistenceEntity> findLatestByDeviceIds(@Param("deviceIds") List<DeviceId> deviceIds);

    @Query("""
            SELECT new com.claircore.analytics.infrastructure.persistence.jpa.repositories.MetricAveragesProjection(
                       AVG(s.averageCo2), AVG(s.averagePm2_5), AVG(s.averageTemperature), AVG(s.averageHumidity))
            FROM DeviceAnalyticsSnapshotPersistenceEntity s
            WHERE s.deviceId = :deviceId AND s.timeWindowStart >= :start AND s.timeWindowStart < :end
            """)
    MetricAveragesProjection findAveragesByDeviceIdAndWindow(
            @Param("deviceId") DeviceId deviceId, @Param("start") Instant start, @Param("end") Instant end);
}
