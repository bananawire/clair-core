package com.claircore.device.application.internal.queryservices;

import com.claircore.device.domain.model.aggregates.Device;
import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.queries.GetDeviceStatusByDeviceIdForUserQuery;
import com.claircore.device.domain.model.valueobjects.*;
import com.claircore.device.domain.repositories.DeviceAssignmentRepository;
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
class DeviceStatusQueryServiceImplTest {

    @Mock
    private DeviceAssignmentRepository deviceAssignmentRepository;

    @Test
    void shouldReturnAssignmentWhenDeviceBelongsToUser() {
        DeviceAssignment assignment = ownedAssignment();
        when(deviceAssignmentRepository.findByDeviceId(assignment.getDeviceId())).thenReturn(Optional.of(assignment));

        Optional<DeviceAssignment> result = new DeviceStatusQueryServiceImpl(deviceAssignmentRepository)
                .handle(new GetDeviceStatusByDeviceIdForUserQuery(assignment.getDeviceId(), assignment.getOwnerUserId()));

        assertEquals(true, result.isPresent());
    }

    @Test
    void shouldReturnEmptyWhenAssignmentDoesNotExist() {
        UUID deviceId = UUID.fromString("550e8400-e29b-41d4-a716-446655441010");
        when(deviceAssignmentRepository.findByDeviceId(deviceId)).thenReturn(Optional.empty());

        Optional<DeviceAssignment> result = new DeviceStatusQueryServiceImpl(deviceAssignmentRepository)
                .handle(new GetDeviceStatusByDeviceIdForUserQuery(deviceId, new UserId(UUID.randomUUID())));

        assertEquals(false, result.isPresent());
    }

    @Test
    void shouldThrowAccessDeniedWhenDeviceDoesNotBelongToUser() {
        DeviceAssignment assignment = ownedAssignment();
        when(deviceAssignmentRepository.findByDeviceId(assignment.getDeviceId())).thenReturn(Optional.of(assignment));

        assertThrowsExactly(
                org.springframework.security.access.AccessDeniedException.class,
                () -> new DeviceStatusQueryServiceImpl(deviceAssignmentRepository)
                        .handle(new GetDeviceStatusByDeviceIdForUserQuery(assignment.getDeviceId(), new UserId(UUID.randomUUID())))
        );
    }

    private DeviceAssignment ownedAssignment() {
        Device device = new Device("SN-0400", "Sensor 0400", new HardwareId("CLAIR-0KBG"), ApiKey.generate(), new DeviceType("air-quality-v1"));
        org.springframework.test.util.ReflectionTestUtils.setField(device, "id", UUID.fromString("550e8400-e29b-41d4-a716-446655441000"));
        DeviceAssignment assignment = new DeviceAssignment(device.getId(), ClaimToken.generate());
        assignment.claimToSpace(UUID.randomUUID(), new UserId(UUID.fromString("550e8400-e29b-41d4-a716-446655441001")));
        return assignment;
    }
}
