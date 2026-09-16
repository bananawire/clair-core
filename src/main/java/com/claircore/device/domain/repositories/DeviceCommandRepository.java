package com.claircore.device.domain.repositories;

import com.claircore.device.domain.model.aggregates.DeviceCommand;
import com.claircore.device.domain.model.valueobjects.DeviceCommandStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port for device command storage. Domain types only. */
public interface DeviceCommandRepository {

    DeviceCommand save(DeviceCommand command);

    Optional<DeviceCommand> findById(UUID id);

    /** Takes a row lock so two acknowledgements of the same command cannot both win. */
    Optional<DeviceCommand> findByIdForAcknowledgement(UUID commandId);

    Optional<DeviceCommand> findByDeviceIdAndCommandId(UUID deviceId, UUID commandId);

    Optional<DeviceCommand> findLatestByDeviceId(UUID deviceId);

    List<DeviceCommand> findByStatusForDispatch(DeviceCommandStatus status, int limit);

    /**
     * Commands the edge should act on: still pending, or sent but past their delivery lease. A null
     * {@code since} means no lower bound.
     */
    List<DeviceCommand> findPendingForEdge(Instant since, Instant leaseCutoff, int limit);

    /** As {@link #findPendingForEdge}, narrowed to one unit. */
    List<DeviceCommand> findPendingForEdgeByHardware(
            String hardwareId, Instant since, Instant leaseCutoff, int limit);

    /**
     * Moves a command to SENT only if it is still claimable and its bound assignment still belongs
     * to the command's device, in one statement.
     *
     * @return 1 when this caller won the claim, 0 when another already had it or the binding is
     * invalid.
     */
    int claimForEdge(UUID commandId, Instant leaseCutoff, Instant claimedAt);
    /**
     * Voids every PENDING or SENT command issued under an assignment that is being unlinked.
     *
     * @return how many commands were expired
     */
    int expireOutstandingByAssignmentId(UUID assignmentId);
}
