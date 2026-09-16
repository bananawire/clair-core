package com.claircore.device.infrastructure.persistence.jpa.repositories;

import com.claircore.device.domain.model.valueobjects.DeviceCommandStatus;
import com.claircore.device.infrastructure.persistence.jpa.entities.DeviceCommandPersistenceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceCommandPersistenceRepository extends JpaRepository<DeviceCommandPersistenceEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM DeviceCommandPersistenceEntity c WHERE c.id = :commandId")
    Optional<DeviceCommandPersistenceEntity> findByIdForAcknowledgement(@Param("commandId") UUID commandId);

    Optional<DeviceCommandPersistenceEntity> findByDeviceIdAndId(UUID deviceId, UUID id);

    Optional<DeviceCommandPersistenceEntity> findFirstByDeviceIdOrderByCreatedAtDesc(UUID deviceId);

    List<DeviceCommandPersistenceEntity> findByStatusOrderByCreatedAtAsc(DeviceCommandStatus status, Pageable pageable);

    /**
     * COALESCE avoids a bare "? IS NULL" placeholder: Postgres cannot infer that parameter's type
     * (no typed context to unify with) and rejects the query with "could not determine data type of
     * parameter $N". Comparing against the column itself when {@code :since} is null keeps the
     * original "no filter" semantics. The edge never sends since, so this runs on every poll.
     *
     * <p>The command does not carry a JPA device association: the caller reads {@code deviceId}
     * off the command and resolves any hardware data separately. The existence predicates below
     * only validate that the device is live and that a bound assignment belongs to that device.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c FROM DeviceCommandPersistenceEntity c
            WHERE ((c.status = com.claircore.device.domain.model.valueobjects.DeviceCommandStatus.PENDING
                        AND c.createdAt >= COALESCE(:since, c.createdAt))
                   OR (c.status = com.claircore.device.domain.model.valueobjects.DeviceCommandStatus.SENT
                        AND c.sentAt <= :leaseCutoff))
              AND EXISTS (SELECT d.id FROM DevicePersistenceEntity d
                           WHERE d.id = c.deviceId AND (d.deleted = false OR d.deleted IS NULL))
              AND (c.assignmentId IS NULL OR EXISTS (SELECT a.id FROM DeviceAssignmentPersistenceEntity a
                                                       WHERE a.id = c.assignmentId
                                                         AND a.deviceId = c.deviceId))
            ORDER BY COALESCE(c.sentAt, c.createdAt) ASC
            """)
    List<DeviceCommandPersistenceEntity> findPendingForEdge(
            @Param("since") Instant since, @Param("leaseCutoff") Instant leaseCutoff, Pageable pageable);

    /** As {@link #findPendingForEdge}, narrowed to the commands of one device. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c FROM DeviceCommandPersistenceEntity c
            WHERE c.deviceId = :deviceId
              AND ((c.status = com.claircore.device.domain.model.valueobjects.DeviceCommandStatus.PENDING
                        AND c.createdAt >= COALESCE(:since, c.createdAt))
                   OR (c.status = com.claircore.device.domain.model.valueobjects.DeviceCommandStatus.SENT
                        AND c.sentAt <= :leaseCutoff))
              AND EXISTS (SELECT d.id FROM DevicePersistenceEntity d
                           WHERE d.id = c.deviceId AND (d.deleted = false OR d.deleted IS NULL))
              AND (c.assignmentId IS NULL OR EXISTS (SELECT a.id FROM DeviceAssignmentPersistenceEntity a
                                                       WHERE a.id = c.assignmentId
                                                         AND a.deviceId = c.deviceId))
            ORDER BY COALESCE(c.sentAt, c.createdAt) ASC
            """)
    List<DeviceCommandPersistenceEntity> findPendingForEdgeByDevice(
            @Param("deviceId") UUID deviceId, @Param("since") Instant since,
            @Param("leaseCutoff") Instant leaseCutoff, Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE DeviceCommandPersistenceEntity c
            SET c.status = com.claircore.device.domain.model.valueobjects.DeviceCommandStatus.SENT,
                c.sentAt = :claimedAt
            WHERE c.id = :commandId
              AND (c.status = com.claircore.device.domain.model.valueobjects.DeviceCommandStatus.PENDING
                   OR (c.status = com.claircore.device.domain.model.valueobjects.DeviceCommandStatus.SENT
                       AND c.sentAt <= :leaseCutoff))
              AND EXISTS (SELECT d.id FROM DevicePersistenceEntity d
                           WHERE d.id = c.deviceId AND (d.deleted = false OR d.deleted IS NULL))
              AND (c.assignmentId IS NULL OR EXISTS (SELECT a.id FROM DeviceAssignmentPersistenceEntity a
                                                       WHERE a.id = c.assignmentId
                                                         AND a.deviceId = c.deviceId))
            """)
    int claimForEdge(@Param("commandId") UUID commandId, @Param("leaseCutoff") Instant leaseCutoff,
                     @Param("claimedAt") Instant claimedAt);
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE DeviceCommandPersistenceEntity c
            SET c.status = com.claircore.device.domain.model.valueobjects.DeviceCommandStatus.EXPIRED
            WHERE c.assignmentId = :assignmentId
              AND c.status IN (com.claircore.device.domain.model.valueobjects.DeviceCommandStatus.PENDING,
                               com.claircore.device.domain.model.valueobjects.DeviceCommandStatus.SENT)
            """)
    int expireOutstandingByAssignmentId(@Param("assignmentId") UUID assignmentId);
}
