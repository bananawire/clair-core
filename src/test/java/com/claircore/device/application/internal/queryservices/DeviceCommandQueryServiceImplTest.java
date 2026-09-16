package com.claircore.device.application.internal.queryservices;

import com.claircore.device.domain.model.aggregates.Device;
import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.aggregates.DeviceCommand;
import com.claircore.device.domain.model.queries.GetDeviceCommandByIdForUserQuery;
import com.claircore.device.domain.model.queries.GetLatestDeviceCommandByDeviceForUserQuery;
import com.claircore.device.domain.model.valueobjects.*;
import com.claircore.device.domain.repositories.DeviceAssignmentRepository;
import com.claircore.device.domain.repositories.DeviceCommandRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceCommandQueryServiceImplTest {

    @Mock
    private DeviceCommandRepository deviceCommandRepository;

    @Mock
    private DeviceAssignmentRepository deviceAssignmentRepository;

    @Test
    void shouldReturnCommandWhenDeviceAndUserMatch() {
        DeviceAssignment assignment = ownedAssignment();
        DeviceCommand command = new DeviceCommand(assignment.getDeviceId(), DeviceCommandType.WAKE, "{}");
        org.springframework.test.util.ReflectionTestUtils.setField(command, "id", UUID.fromString("550e8400-e29b-41d4-a716-446655441100"));
        when(deviceAssignmentRepository.findByDeviceId(assignment.getDeviceId())).thenReturn(Optional.of(assignment));
        when(deviceCommandRepository.findByDeviceIdAndCommandId(assignment.getDeviceId(), command.getId())).thenReturn(Optional.of(command));

        Optional<DeviceCommand> result = new DeviceCommandQueryServiceImpl(deviceCommandRepository, deviceAssignmentRepository)
                .handle(new GetDeviceCommandByIdForUserQuery(assignment.getDeviceId(), command.getId(), assignment.getOwnerUserId()));

        assertEquals(true, result.isPresent());
    }

    @Test
    void shouldReturnLatestCommandWhenDeviceAndUserMatch() {
        DeviceAssignment assignment = ownedAssignment();
        DeviceCommand command = new DeviceCommand(assignment.getDeviceId(), DeviceCommandType.RESTART, "{}");
        when(deviceAssignmentRepository.findByDeviceId(assignment.getDeviceId())).thenReturn(Optional.of(assignment));
        when(deviceCommandRepository.findLatestByDeviceId(assignment.getDeviceId())).thenReturn(Optional.of(command));

        Optional<DeviceCommand> result = new DeviceCommandQueryServiceImpl(deviceCommandRepository, deviceAssignmentRepository)
                .handle(new GetLatestDeviceCommandByDeviceForUserQuery(assignment.getDeviceId(), assignment.getOwnerUserId()));

        assertEquals(true, result.isPresent());
    }

    @Test
    void shouldThrowAccessDeniedWhenDeviceDoesNotBelongToUser() {
        DeviceAssignment assignment = ownedAssignment();
        when(deviceAssignmentRepository.findByDeviceId(assignment.getDeviceId())).thenReturn(Optional.of(assignment));

        assertThrowsExactly(
                org.springframework.security.access.AccessDeniedException.class,
                () -> new DeviceCommandQueryServiceImpl(deviceCommandRepository, deviceAssignmentRepository)
                        .handle(new GetLatestDeviceCommandByDeviceForUserQuery(assignment.getDeviceId(), new UserId(UUID.randomUUID())))
        );
    }

    private DeviceAssignment ownedAssignment() {
        Device device = new Device("SN-0500", "Sensor 0500", new HardwareId("CLAIR-0KBG"), ApiKey.generate(), new DeviceType("air-quality-v1"));
        org.springframework.test.util.ReflectionTestUtils.setField(device, "id", UUID.fromString("550e8400-e29b-41d4-a716-446655441101"));
        DeviceAssignment assignment = new DeviceAssignment(device.getId(), ClaimToken.generate());
        assignment.claimToSpace(UUID.randomUUID(), new UserId(UUID.fromString("550e8400-e29b-41d4-a716-446655441102")));
        return assignment;
    }
}
