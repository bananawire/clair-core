package com.claircore.device.application.acl;

import com.claircore.device.application.commandservices.DeviceControlCommandService;
import com.claircore.device.application.queryservices.DeviceCommandQueryService;
import com.claircore.device.application.queryservices.DeviceQueryService;
import com.claircore.device.domain.model.aggregates.DeviceCommand;
import com.claircore.device.domain.model.commands.AcknowledgeDeviceCommandCommand;
import com.claircore.device.domain.model.valueobjects.DeviceCommandStatus;
import com.claircore.device.domain.model.valueobjects.DeviceCommandType;
import com.claircore.device.interfaces.acl.DeviceCommandForEdge;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceContextFacadeImplCommandTest {

    @Mock
    private DeviceQueryService deviceQueryService;
    @Mock
    private DeviceCommandQueryService deviceCommandQueryService;
    @Mock
    private DeviceControlCommandService deviceControlCommandService;

    @Test
    void shouldExposeClaimableCommandsAsStableAclData() {
        DeviceCommand command = command(DeviceCommandStatus.PENDING, DeviceCommandType.STANDBY);
        Instant cutoff = Instant.parse("2026-05-16T22:29:00Z");
        when(deviceCommandQueryService.findClaimableForEdge(cutoff, 10)).thenReturn(List.of(command));

        var facade = facade();
        List<DeviceCommandForEdge> result = facade.findClaimableCommands(cutoff, 10);

        assertThat(result).singleElement().satisfies(edgeCommand -> {
            assertThat(edgeCommand.commandId()).isEqualTo(command.getId());
            assertThat(edgeCommand.deviceId()).isEqualTo(command.getDeviceId());
            assertThat(edgeCommand.assignmentId()).isEqualTo(command.getAssignmentId());
            assertThat(edgeCommand.type()).isEqualTo("STANDBY");
        });
    }

    @Test
    void shouldMapClaimedCommandWithoutLeakingDeviceDomainType() {
        DeviceCommand command = command(DeviceCommandStatus.SENT, DeviceCommandType.RESTART);
        Instant cutoff = Instant.parse("2026-05-16T22:29:00Z");
        Instant claimedAt = Instant.parse("2026-05-16T22:30:00Z");
        when(deviceControlCommandService.claimForEdge(command.getId(), cutoff, claimedAt))
                .thenReturn(Optional.of(command));

        Optional<DeviceCommandForEdge> result = facade().claimCommand(command.getId(), cutoff, claimedAt);

        assertThat(result).get().extracting(DeviceCommandForEdge::type).isEqualTo("RESTART");
    }

    @Test
    void shouldTranslateSuccessfulAckIntoTheDeviceApplicationCommand() {
        DeviceCommand command = command(DeviceCommandStatus.EXECUTED, DeviceCommandType.WAKE);
        when(deviceControlCommandService.handle(any(AcknowledgeDeviceCommandCommand.class)))
                .thenReturn(command);

        Optional<DeviceCommandForEdge> result = facade().acknowledgeCommand(
                command.getDeviceId(), command.getId(), "EXECUTED", null);

        assertThat(result).isPresent();
        ArgumentCaptor<AcknowledgeDeviceCommandCommand> captor =
                ArgumentCaptor.forClass(AcknowledgeDeviceCommandCommand.class);
        verify(deviceControlCommandService).handle(captor.capture());
        assertThat(captor.getValue().deviceId()).isEqualTo(command.getDeviceId());
        assertThat(captor.getValue().commandId()).isEqualTo(command.getId());
        assertThat(captor.getValue().status()).isEqualTo(DeviceCommandStatus.EXECUTED);
    }

    @Test
    void shouldRejectAnUnknownAckStatusAtTheAclBoundary() {
        UUID deviceId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();

        assertThatThrownBy(() -> facade().acknowledgeCommand(deviceId, commandId, "UNKNOWN", null))
                .isInstanceOf(IllegalArgumentException.class);

        verify(deviceControlCommandService, never()).handle(any(AcknowledgeDeviceCommandCommand.class));
    }

    private DeviceContextFacadeImpl facade() {
        return new DeviceContextFacadeImpl(
                deviceQueryService, deviceCommandQueryService, deviceControlCommandService);
    }

    private static DeviceCommand command(DeviceCommandStatus status, DeviceCommandType type) {
        return DeviceCommand.reconstitute(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), type, status,
                "{}", Instant.parse("2026-05-16T22:29:00Z"), null, null,
                Instant.parse("2026-05-16T22:28:00Z"), Instant.parse("2026-05-16T22:29:00Z"));
    }
}
