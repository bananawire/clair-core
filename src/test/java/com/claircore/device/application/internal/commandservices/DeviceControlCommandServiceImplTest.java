package com.claircore.device.application.internal.commandservices;

import com.claircore.device.domain.model.commands.AcknowledgeDeviceCommandCommand;
import com.claircore.device.domain.model.commands.CreateDeviceCommandCommand;
import com.claircore.device.domain.model.commands.DispatchPendingDeviceCommandsCommand;
import com.claircore.device.domain.model.aggregates.Device;
import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.aggregates.DeviceCommand;
import com.claircore.device.domain.model.valueobjects.*;
import com.claircore.device.domain.repositories.DeviceAssignmentRepository;
import com.claircore.device.domain.repositories.DeviceCommandRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceControlCommandServiceImplTest {

    @Mock
    private DeviceAssignmentRepository deviceAssignmentRepository;

    @Mock
    private DeviceCommandRepository deviceCommandRepository;

    @Mock
    private com.claircore.device.domain.repositories.DeviceRepository deviceRepository;

    private DeviceControlCommandServiceImpl service() {
        return new DeviceControlCommandServiceImpl(
                deviceAssignmentRepository,
                deviceCommandRepository,
                deviceRepository
        );
    }

    @Test
    void shouldCreateDeviceCommandWhenDeviceBelongsToUser() {
        DeviceAssignment assignment = ownedAssignment();
        when(deviceAssignmentRepository.findByDeviceIdForUpdate(assignment.getDeviceId())).thenReturn(Optional.of(assignment));
        when(deviceRepository.findById(assignment.getDeviceId())).thenReturn(Optional.of(ownedDevice()));
        when(deviceCommandRepository.save(any(DeviceCommand.class))).thenAnswer(invocation -> {
            DeviceCommand saved = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(saved, "id", UUID.fromString("550e8400-e29b-41d4-a716-446655443500"));
            return saved;
        });

        DeviceCommand result = service().handle(new CreateDeviceCommandCommand(
                assignment.getDeviceId(),
                DeviceCommandType.WAKE,
                "{}",
                assignment.getOwnerUserId()
        ));

        assertEquals(DeviceCommandStatus.PENDING, result.getStatus());
        assertEquals(assignment.getId(), result.getAssignmentId());
        verify(deviceCommandRepository).save(any(DeviceCommand.class));
    }

    @Test
    void shouldThrowAccessDeniedWhenDeviceDoesNotBelongToUser() {
        DeviceAssignment assignment = ownedAssignment();
        when(deviceAssignmentRepository.findByDeviceIdForUpdate(assignment.getDeviceId())).thenReturn(Optional.of(assignment));
        // Ownership is refused before the device is ever looked up.

        assertThrowsExactly(
                org.springframework.security.access.AccessDeniedException.class,
                () -> service().handle(new CreateDeviceCommandCommand(
                                assignment.getDeviceId(),
                                DeviceCommandType.WAKE,
                                "{}",
                                new UserId(UUID.randomUUID())
                        ))
        );

        verify(deviceCommandRepository, never()).save(any());
    }

    @Test
    void shouldMarkCommandsAsSentWhenDispatchingPendingCommands() {
        DeviceCommand first = new DeviceCommand(ownedAssignment().getDeviceId(), DeviceCommandType.STANDBY, "{}");
        DeviceCommand second = new DeviceCommand(ownedAssignment().getDeviceId(), DeviceCommandType.RESTART, "{}");
        when(deviceCommandRepository.findByStatusForDispatch(DeviceCommandStatus.PENDING, 2))
                .thenReturn(List.of(first, second));
        when(deviceCommandRepository.save(any(DeviceCommand.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<DeviceCommand> result = service().handle(new DispatchPendingDeviceCommandsCommand(2));

        assertEquals(DeviceCommandStatus.SENT, result.get(0).getStatus());
        assertEquals(DeviceCommandStatus.SENT, result.get(1).getStatus());
        verify(deviceCommandRepository).save(first);
        verify(deviceCommandRepository).save(second);
    }

    @Test
    void shouldClaimPendingCommandAndReturnTheSentCommand() {
        DeviceAssignment assignment = ownedAssignment();
        DeviceCommand command = new DeviceCommand(
                assignment.getDeviceId(), assignment.getId(), DeviceCommandType.WAKE, "{}");
        UUID commandId = UUID.randomUUID();
        org.springframework.test.util.ReflectionTestUtils.setField(command, "id", commandId);
        command.markSent();
        Instant cutoff = Instant.parse("2026-05-16T22:29:00Z");
        Instant claimedAt = Instant.parse("2026-05-16T22:30:00Z");
        when(deviceCommandRepository.claimForEdge(commandId, cutoff, claimedAt)).thenReturn(1);
        when(deviceCommandRepository.findById(commandId)).thenReturn(Optional.of(command));

        Optional<DeviceCommand> result = service().claimForEdge(commandId, cutoff, claimedAt);

        assertEquals(Optional.of(command), result);
        assertEquals(DeviceCommandStatus.SENT, result.orElseThrow().getStatus());
        verify(deviceCommandRepository).claimForEdge(commandId, cutoff, claimedAt);
        verify(deviceCommandRepository).findById(commandId);
    }

    @Test
    void shouldNotReadOrExecuteACommandWhenClaimWasLostToAnotherConsumer() {
        UUID commandId = UUID.randomUUID();
        Instant cutoff = Instant.parse("2026-05-16T22:29:00Z");
        Instant claimedAt = Instant.parse("2026-05-16T22:30:00Z");
        when(deviceCommandRepository.claimForEdge(commandId, cutoff, claimedAt)).thenReturn(0);

        Optional<DeviceCommand> result = service().claimForEdge(commandId, cutoff, claimedAt);

        assertEquals(Optional.empty(), result);
        verify(deviceCommandRepository, never()).findById(commandId);
    }

    @Test
    void shouldLeaveAAlreadyTerminalAckIdempotent() {
        DeviceAssignment assignment = ownedAssignment();
        DeviceCommand command = new DeviceCommand(
                assignment.getDeviceId(), assignment.getId(), DeviceCommandType.WAKE, "{}");
        UUID commandId = UUID.randomUUID();
        org.springframework.test.util.ReflectionTestUtils.setField(command, "id", commandId);
        command.markExecuted();
        when(deviceCommandRepository.findByIdForAcknowledgement(commandId)).thenReturn(Optional.of(command));

        DeviceCommand result = service().handle(new AcknowledgeDeviceCommandCommand(
                assignment.getDeviceId(), commandId, DeviceCommandStatus.FAILED, "late failure"));

        assertEquals(DeviceCommandStatus.EXECUTED, result.getStatus());
        verify(deviceAssignmentRepository, never()).findByDeviceIdForUpdate(any());
        verify(deviceCommandRepository, never()).save(any(DeviceCommand.class));
    }

    @Test
    void shouldRejectAnAckForAnotherDevice() {
        DeviceAssignment assignment = ownedAssignment();
        DeviceCommand command = new DeviceCommand(
                assignment.getDeviceId(), assignment.getId(), DeviceCommandType.WAKE, "{}");
        UUID commandId = UUID.randomUUID();
        org.springframework.test.util.ReflectionTestUtils.setField(command, "id", commandId);
        when(deviceCommandRepository.findByIdForAcknowledgement(commandId)).thenReturn(Optional.of(command));

        assertThrowsExactly(IllegalArgumentException.class, () -> service().handle(
                new AcknowledgeDeviceCommandCommand(
                        UUID.randomUUID(), commandId, DeviceCommandStatus.EXECUTED, null)));

        verify(deviceAssignmentRepository, never()).findByDeviceIdForUpdate(any());
        verify(deviceCommandRepository, never()).save(any(DeviceCommand.class));
    }

    @Test
    void shouldMarkCommandAsExecutedWhenAcknowledgedSuccessfully() {
        DeviceAssignment assignment = ownedAssignment();
        DeviceCommand command = new DeviceCommand(assignment.getDeviceId(), DeviceCommandType.WAKE, "{}");
        org.springframework.test.util.ReflectionTestUtils.setField(command, "id", UUID.fromString("550e8400-e29b-41d4-a716-446655440900"));
        when(deviceCommandRepository.findByDeviceIdAndCommandId(assignment.getDeviceId(), command.getId())).thenReturn(Optional.of(command));
        when(deviceAssignmentRepository.findByDeviceIdForUpdate(assignment.getDeviceId())).thenReturn(Optional.of(assignment));
        when(deviceCommandRepository.save(any(DeviceCommand.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceCommand result = service().handle(new AcknowledgeDeviceCommandCommand(assignment.getDeviceId(), command.getId(), DeviceCommandStatus.EXECUTED, null));

        assertEquals(DeviceCommandStatus.EXECUTED, result.getStatus());
        verify(deviceAssignmentRepository).save(assignment);
    }

    @Test
    void shouldMarkCommandAsFailedWhenEdgeReportsExecutionFailure() {
        DeviceAssignment assignment = ownedAssignment();
        DeviceCommand command = new DeviceCommand(
                assignment.getDeviceId(), assignment.getId(), DeviceCommandType.RESTART, "{}");
        UUID commandId = UUID.randomUUID();
        org.springframework.test.util.ReflectionTestUtils.setField(command, "id", commandId);
        when(deviceCommandRepository.findByIdForAcknowledgement(commandId)).thenReturn(Optional.of(command));
        when(deviceAssignmentRepository.findByDeviceIdForUpdate(assignment.getDeviceId()))
                .thenReturn(Optional.of(assignment));
        when(deviceCommandRepository.save(any(DeviceCommand.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DeviceCommand result = service().handle(new AcknowledgeDeviceCommandCommand(
                assignment.getDeviceId(), commandId, DeviceCommandStatus.FAILED, "actuator unavailable"));

        assertEquals(DeviceCommandStatus.FAILED, result.getStatus());
        assertEquals("actuator unavailable", result.getFailureReason());
        verify(deviceAssignmentRepository, never()).save(any(DeviceAssignment.class));
        verify(deviceCommandRepository).save(command);
    }

    @Test
    void aLegacyAcknowledgementForAPreviousGenerationIsRejectedAndTheCommandExpired() {
        DeviceAssignment current = ownedAssignment();
        DeviceCommand stale = new DeviceCommand(current.getDeviceId(), UUID.randomUUID(), DeviceCommandType.STANDBY, "{}");
        org.springframework.test.util.ReflectionTestUtils.setField(stale, "id", UUID.fromString("550e8400-e29b-41d4-a716-446655440910"));
        when(deviceCommandRepository.findByDeviceIdAndCommandId(current.getDeviceId(), stale.getId())).thenReturn(Optional.of(stale));
        when(deviceAssignmentRepository.findByDeviceIdForUpdate(current.getDeviceId())).thenReturn(Optional.of(current));
        assertThrowsExactly(IllegalStateException.class, () ->
                service().handle(new AcknowledgeDeviceCommandCommand(current.getDeviceId(), stale.getId(), DeviceCommandStatus.EXECUTED, null)));
        assertEquals(DeviceCommandStatus.EXPIRED, stale.getStatus());
        assertEquals(DeviceStatus.OFFLINE, current.getStatus());
        verify(deviceAssignmentRepository, never()).save(any());
        verify(deviceCommandRepository).save(stale);
    }

    /** The service resolves the device through its own port now, so tests must stub that lookup. */
    private Device ownedDevice() {
        Device device = new Device(
                "SN-0300",
                "Sensor 0300",
                new HardwareId("CLAIR-0KBG"),
                ApiKey.generate(),
                new DeviceType("air-quality-v1")
        );
        org.springframework.test.util.ReflectionTestUtils.setField(device, "id", UUID.fromString("550e8400-e29b-41d4-a716-446655440901"));
        return device;
    }

    private DeviceAssignment ownedAssignment() {
        Device device = new Device(
                "SN-0300",
                "Sensor 0300",
                new HardwareId("CLAIR-0KBG"),
                ApiKey.generate(),
                new DeviceType("air-quality-v1")
        );
        org.springframework.test.util.ReflectionTestUtils.setField(device, "id", UUID.fromString("550e8400-e29b-41d4-a716-446655440901"));
        DeviceAssignment assignment = new DeviceAssignment(device.getId(), ClaimToken.generate());
        assignment.claimToSpace(UUID.fromString("550e8400-e29b-41d4-a716-446655440902"), new UserId(UUID.fromString("550e8400-e29b-41d4-a716-446655440903")));
        return assignment;
    }
}
