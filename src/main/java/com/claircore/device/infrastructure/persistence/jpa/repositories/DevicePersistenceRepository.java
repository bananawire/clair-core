package com.claircore.device.infrastructure.persistence.jpa.repositories;

import com.claircore.device.domain.model.valueobjects.ApiKey;
import com.claircore.device.domain.model.valueobjects.HardwareId;
import com.claircore.device.infrastructure.persistence.jpa.entities.DevicePersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DevicePersistenceRepository extends JpaRepository<DevicePersistenceEntity, UUID> {

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE DevicePersistenceEntity d SET d.updatedAt = CASE WHEN d.updatedAt > :watermark THEN d.updatedAt ELSE :watermark END WHERE d.id = :deviceId")
    int advanceRosterWatermark(@Param("deviceId") UUID deviceId, @Param("watermark") Instant watermark);

    Optional<DevicePersistenceEntity> findBySerialNumber(String serialNumber);

    List<DevicePersistenceEntity> findAllBySerialNumberIn(Collection<String> serialNumbers);

    Optional<DevicePersistenceEntity> findByHardwareId(HardwareId hardwareId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DevicePersistenceEntity d WHERE d.hardwareId = :hardwareId")
    Optional<DevicePersistenceEntity> findByHardwareIdForUpdate(@Param("hardwareId") HardwareId hardwareId);

    Optional<DevicePersistenceEntity> findByApiKey(ApiKey apiKey);

    boolean existsByHardwareId(HardwareId hardwareId);

    /**
     * Page of devices ordered by id, skipping tombstoned rows. Uses {@code PageRequest.of} to
     * cap the page size and let the LocalEdge pull bounded batches in one query.
     */
    org.springframework.data.domain.Slice<DevicePersistenceEntity> findAllByDeletedFalseOrderByIdAsc(
            org.springframework.data.domain.Pageable pageable);

    /**
     * Page of devices ordered by id, including tombstoned rows. Used by edge-style callers that
     * want to reason about decommissions as part of the page result.
     */
    org.springframework.data.domain.Slice<DevicePersistenceEntity> findAllByOrderByIdAsc(
            org.springframework.data.domain.Pageable pageable);

}
