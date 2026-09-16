package com.claircore.device.application.queryservices;

import com.claircore.device.domain.model.aggregates.Device;
import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.aggregates.Organization;
import com.claircore.device.domain.model.aggregates.Space;
import com.claircore.device.domain.model.queries.*;
import com.claircore.shared.domain.model.PageResult;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceQueryService {
    Optional<Organization> handle(GetOrganizationByIdQuery query);
    List<Organization> handle(GetOrganizationsByOwnerQuery query);
    Optional<Space> handle(GetSpaceByIdQuery query);
    /** Owner-scoped reads. The unscoped variants above stay for trusted internal callers only. */
    Optional<Space> handle(GetSpaceByIdForUserQuery query);
    List<Space> handle(GetSpacesByOrganizationForUserQuery query);
    Optional<Organization> handle(GetOrganizationByIdForUserQuery query);
    PageResult<AssignedDevice> handle(GetDevicesBySpaceForUserQuery query);
    Optional<AssignedDevice> handle(GetAssignedDeviceByIdForUserQuery query);
    List<Space> handle(GetSpacesByOrganizationQuery query);
    Optional<Device> handle(GetDeviceByIdQuery query);
    Optional<Device> handle(GetDeviceBySerialNumberQuery query);
    Optional<Device> handle(GetDeviceByHardwareIdQuery query);
    Optional<Device> handle(GetDeviceByApiKeyQuery query);
    PageResult<AssignedDevice> handle(GetDevicesBySpaceQuery query);
    List<Device> handle(GetProvisionedDevicesQuery query);
    PageResult<Device> handle(GetDevicesForTelemetryQuery query);
    Optional<UUID> findSpaceIdByDeviceId(UUID deviceId);
    boolean isDeviceOwnedByUser(UUID deviceId, UUID userId);
    Optional<UUID> findOwnerIdByDeviceId(UUID deviceId);
    boolean isSpaceOwnedByUser(UUID spaceId, UUID userId);
    List<UUID> findDeviceIdsByOwnerId(UUID ownerUserId);
    Optional<DeviceAssignment> findAssignmentByDeviceId(UUID deviceId);

    /**
     * Pessimistic-locked variant used by presence writes: {@code LocalEdge} triggers one per
     * simulated cycle and the lock prevents two concurrent updates from being reordered.
     */
    Optional<DeviceAssignment> findAssignmentByDeviceIdForUpdate(UUID deviceId);
    /** When the current owner claimed the device; empty while unclaimed. Older readings belong to a previous owner. */
    Optional<java.time.Instant> findActivatedAtByDeviceId(UUID deviceId);

    /** The assignment together with the device it points at, resolved in one place. */
    Optional<AssignedDevice> findAssignedDeviceByDeviceId(UUID deviceId);

    /**
     * Batch lookup to avoid N+1 queries in read models.
     */
    Map<UUID, String> findDeviceNamesByDeviceIds(List<UUID> deviceIds);

    Map<UUID, String> findHardwareIdsByDeviceIds(List<UUID> deviceIds);

    /**
     * Batch lookup to avoid N+1 queries in read models.
     */
    Map<UUID, String> findSpaceNamesBySpaceIds(List<UUID> spaceIds);

    /**
     * A device and where it is assigned. The two used to travel as one object through a lazy
     * association; pairing them explicitly is what lets the REST layer render both outside a
     * transaction.
     */
    record AssignedDevice(DeviceAssignment assignment, Device device) {}

    /**
     * Apply an occurrence-order presence event to the device's {@code DeviceAssignment}. The
     * {@code LocalEdge} ACL publishes one of these per simulated reading; failures surface to
     * the caller as a runtime exception.
     */
    void updatePresence(UUID deviceId, com.claircore.device.domain.model.valueobjects.DeviceStatus status, java.time.Instant occurredAt);
}
