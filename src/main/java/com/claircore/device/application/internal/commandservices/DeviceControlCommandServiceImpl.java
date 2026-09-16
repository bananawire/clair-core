package com.claircore.device.application.internal.commandservices;

import com.claircore.device.domain.model.commands.AcknowledgeDeviceCommandCommand;
import com.claircore.device.domain.model.commands.CreateDeviceCommandCommand;
import com.claircore.device.domain.model.commands.DispatchPendingDeviceCommandsCommand;
import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.aggregates.DeviceCommand;
import com.claircore.device.domain.model.valueobjects.DeviceCommandStatus;
import com.claircore.device.application.commandservices.DeviceControlCommandService;
import com.claircore.device.domain.repositories.DeviceAssignmentRepository;
import com.claircore.device.domain.repositories.DeviceRepository;
import com.claircore.device.domain.repositories.DeviceCommandRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues STANDBY/WAKE/RESTART commands against a device and applies the acknowledgements that
 * come back.
 *
 * <p>The previous implementation also fanned out to an edge webhook publisher; that integration
 * is gone and the command lifecycle now stops at the in-app write side. The {@code Sent} status
 * is preserved because the existing schema (and tests around it) depend on the field being set
 * even though the literal "edge" target is no longer external.
 */
@Service
public class DeviceControlCommandServiceImpl implements DeviceControlCommandService {

    private final DeviceAssignmentRepository deviceAssignmentRepository;
    private final DeviceCommandRepository deviceCommandRepository;
    private final DeviceRepository deviceRepository;

    public DeviceControlCommandServiceImpl(
            DeviceAssignmentRepository deviceAssignmentRepository,
            DeviceCommandRepository deviceCommandRepository,
            DeviceRepository deviceRepository
    ) {
        this.deviceAssignmentRepository = deviceAssignmentRepository;
        this.deviceCommandRepository = deviceCommandRepository;
        this.deviceRepository = deviceRepository;
    }

    @Override
    @Transactional
    public DeviceCommand handle(CreateDeviceCommandCommand command) {
        // Locked: a reset running concurrently must either see this command and expire it, or
        // finish first so this creation fails on the missing assignment. Never both succeed.
        DeviceAssignment assignment = deviceAssignmentRepository
                .findByDeviceIdForUpdate(command.deviceId())
                .orElseThrow(() -> new IllegalArgumentException("Device assignment not found"));

        if (assignment.getOwnerUserId() == null || !assignment.getOwnerUserId().equals(command.userId())) {
            throw new AccessDeniedException("Device does not belong to user");
        }

        requireDevice(assignment.getDeviceId());

        DeviceCommand deviceCommand = new DeviceCommand(
                assignment.getDeviceId(), assignment.getId(), command.type(), command.payload());
        return deviceCommandRepository.save(deviceCommand);
    }

    @Override
    @Transactional
    public List<DeviceCommand> handle(DispatchPendingDeviceCommandsCommand command) {
        int limit = command.limit() == null ? 100 : Math.min(command.limit(), 500);
        List<DeviceCommand> commands = deviceCommandRepository.findByStatusForDispatch(
                DeviceCommandStatus.PENDING, limit);
        commands.forEach(DeviceCommand::markSent);
        return commands.stream().map(deviceCommandRepository::save).toList();
    }

    @Override
    @Transactional
    public Optional<DeviceCommand> claimForEdge(UUID commandId, Instant leaseCutoff, Instant claimedAt) {
        if (commandId == null || leaseCutoff == null || claimedAt == null) {
            throw new IllegalArgumentException("commandId, leaseCutoff, and claimedAt are required");
        }
        if (deviceCommandRepository.claimForEdge(commandId, leaseCutoff, claimedAt) == 0) {
            return Optional.empty();
        }
        return deviceCommandRepository.findById(commandId);
    }

    @Override
    @Transactional
    public DeviceCommand handle(AcknowledgeDeviceCommandCommand command) {
        Optional<DeviceCommand> locked = deviceCommandRepository.findByIdForAcknowledgement(command.commandId());
        // The fallback keeps lightweight adapters compatible; the JPA adapter returns the
        // pessimistically locked lookup above. A real command cannot disappear between these
        // lookups without also becoming unusable, while test/double adapters may not implement
        // the lock-aware method.
        if (locked == null || locked.isEmpty()) {
            locked = deviceCommandRepository.findByDeviceIdAndCommandId(command.deviceId(), command.commandId());
        }
        DeviceCommand deviceCommand = locked
                .filter(found -> found.getDeviceId().equals(command.deviceId()))
                .orElseThrow(() -> new IllegalArgumentException("Device command not found"));
        if (deviceCommand.getStatus() == DeviceCommandStatus.EXECUTED
                || deviceCommand.getStatus() == DeviceCommandStatus.FAILED
                || deviceCommand.getStatus() == DeviceCommandStatus.EXPIRED) {
            return deviceCommand;
        }
        DeviceAssignment assignment = deviceAssignmentRepository
                .findByDeviceIdForUpdate(deviceCommand.getDeviceId())
                .orElseThrow(() -> new IllegalArgumentException("Device assignment not found"));
        if (!deviceCommand.belongsToAssignment(assignment.getId())) {
            // Issued under a previous owner: the result must not leak onto the current assignment.
            deviceCommand.expire();
            deviceCommandRepository.save(deviceCommand);
            throw new IllegalStateException("Device command belongs to a previous assignment");
        }
        if (command.status() == DeviceCommandStatus.EXECUTED) {
            deviceCommand.markExecuted();
            applyExecutedCommandToDevice(deviceCommand, assignment);
        } else {
            deviceCommand.markFailed(command.failureReason());
        }
        return deviceCommandRepository.save(deviceCommand);
    }

    private void applyExecutedCommandToDevice(DeviceCommand deviceCommand, DeviceAssignment assignment) {
        switch (deviceCommand.getType()) {
            case STANDBY -> assignment.markStandby();
            case WAKE -> assignment.markOnline();
            case RESTART -> assignment.markOnline();
        }
        deviceAssignmentRepository.save(assignment);
    }

    private void requireDevice(java.util.UUID deviceId) {
        deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found"));
    }
}
