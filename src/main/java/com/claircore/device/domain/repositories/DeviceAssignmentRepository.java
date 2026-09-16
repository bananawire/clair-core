package com.claircore.device.domain.repositories;

import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.shared.domain.model.PageResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port for device assignment storage. Domain types only. */
public interface DeviceAssignmentRepository {

    DeviceAssignment save(DeviceAssignment assignment);

    Optional<DeviceAssignment> findById(UUID id);

    PageResult<DeviceAssignment> findBySpaceId(UUID spaceId, int page, int size);

    long countBySpaceId(UUID spaceId);

    boolean existsBySpaceId(UUID spaceId);

    Optional<DeviceAssignment> findByDeviceId(UUID deviceId);

    /**
     * Takes a row lock for the duration of the transaction. Claiming and presence updates race with
     * each other, and the lock is what makes "already claimed" a reliable check rather than a guess.
     */
    Optional<DeviceAssignment> findByDeviceIdForUpdate(UUID deviceId);

    Optional<DeviceAssignment> findByClaimToken(String claimToken);
    /** As {@link #findByClaimToken}, holding a row lock so one token can be consumed only once. */
    Optional<DeviceAssignment> findByClaimTokenForUpdate(String claimToken);
    /**
     * Serializes every claim of one owner for the rest of the transaction, so two concurrent claims
     * cannot both read a count under the quota and both succeed. Implementations that cannot lock
     * per owner may no-op; the quota is then best effort on that database.
     */
    void lockOwnerQuotaBoundary(UserId ownerUserId);

    boolean existsByOrganizationId(UUID organizationId);

    long countByOwnerUserId(UserId ownerUserId);

    List<UUID> findDeviceIdsByOwnerUserId(UserId ownerUserId);

    boolean existsByDeviceIdAndOwnerUserId(UUID deviceId, UserId ownerUserId);

    /** Unlinks a device: the assignment row goes, the device row stays. */
    void deleteById(UUID id);
}
