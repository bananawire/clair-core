package com.claircore.device.infrastructure.persistence.jpa.repositories;

import com.claircore.device.domain.model.valueobjects.ClaimToken;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.infrastructure.persistence.jpa.entities.DeviceAssignmentPersistenceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceAssignmentPersistenceRepository
        extends JpaRepository<DeviceAssignmentPersistenceEntity, UUID> {

    Page<DeviceAssignmentPersistenceEntity> findBySpaceId(UUID spaceId, Pageable pageable);

    long countBySpaceId(UUID spaceId);

    boolean existsBySpaceId(UUID spaceId);

    Optional<DeviceAssignmentPersistenceEntity> findByDeviceId(UUID deviceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM DeviceAssignmentPersistenceEntity a WHERE a.deviceId = :deviceId")
    Optional<DeviceAssignmentPersistenceEntity> findByDeviceIdForUpdate(@Param("deviceId") UUID deviceId);

    Optional<DeviceAssignmentPersistenceEntity> findByClaimToken(ClaimToken claimToken);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM DeviceAssignmentPersistenceEntity a WHERE a.claimToken = :claimToken")
    Optional<DeviceAssignmentPersistenceEntity> findByClaimTokenForUpdate(@Param("claimToken") ClaimToken claimToken);

    @Query("""
            SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
            FROM DeviceAssignmentPersistenceEntity a
            WHERE a.spaceId IN (SELECT s.id FROM SpacePersistenceEntity s WHERE s.organizationId = :organizationId)
            """)
    boolean existsByOrganizationId(@Param("organizationId") UUID organizationId);

    @Query("""
            SELECT COUNT(a) FROM DeviceAssignmentPersistenceEntity a
            WHERE a.spaceId IN (SELECT s.id FROM SpacePersistenceEntity s WHERE s.ownerUserId = :ownerUserId)
            """)
    long countByOwnerUserId(@Param("ownerUserId") UserId ownerUserId);

    @Query("SELECT a.deviceId FROM DeviceAssignmentPersistenceEntity a WHERE a.ownerUserId = :ownerUserId")
    List<UUID> findDeviceIdsByOwnerUserId(@Param("ownerUserId") UserId ownerUserId);

    boolean existsByDeviceIdAndOwnerUserId(UUID deviceId, UserId ownerUserId);
}
