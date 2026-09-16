package com.claircore.device.application.internal.commandservices;

import com.claircore.device.domain.model.commands.RemoveDeviceThresholdCommand;
import com.claircore.device.domain.model.commands.WriteDeviceThresholdCommand;
import com.claircore.device.domain.model.aggregates.Device;
import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.valueobjects.*;
import com.claircore.device.domain.repositories.DeviceAssignmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceThresholdCommandServiceImplTest {

    @Mock
    private DeviceAssignmentRepository deviceAssignmentRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateThresholdWhenMetricDoesNotExist() {
        DeviceAssignment assignment = ownedAssignment();
        when(deviceAssignmentRepository.findByDeviceIdForUpdate(assignment.getDeviceId())).thenReturn(Optional.of(assignment));
        when(deviceAssignmentRepository.save(any(DeviceAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceMetricThresholdConfiguration configuration = new DeviceThresholdCommandServiceImpl(deviceAssignmentRepository, objectMapper)
                .handle(new WriteDeviceThresholdCommand(
                        assignment.getDeviceId(),
                        assignment.getOwnerUserId(),
                        MetricThreshold.PM25,
                        new BigDecimal("35.5"),
                        true,
                        DeviceThresholdWriteIntent.CREATE
                ));

        assertEquals(MetricThreshold.PM25, configuration.metric());
        verify(deviceAssignmentRepository).save(assignment);
    }

    @Test
    void shouldThrowExceptionWhenThresholdAlreadyExists() {
        DeviceAssignment assignment = ownedAssignment();
        assignment.putConfigurationValue("threshold.PM25", "{\"metric\":\"PM25\",\"value\":35.5,\"enabled\":true}");
        when(deviceAssignmentRepository.findByDeviceIdForUpdate(assignment.getDeviceId())).thenReturn(Optional.of(assignment));

        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new DeviceThresholdCommandServiceImpl(deviceAssignmentRepository, objectMapper)
                        .handle(new WriteDeviceThresholdCommand(
                                assignment.getDeviceId(),
                                assignment.getOwnerUserId(),
                                MetricThreshold.PM25,
                                new BigDecimal("40.0"),
                                true,
                                DeviceThresholdWriteIntent.CREATE
                        ))
        );

        assertEquals("Threshold already exists for the specified metric", exception.getMessage());
        verify(deviceAssignmentRepository, never()).save(any());
    }

    @Test
    void shouldRemoveThresholdWhenMetricExists() {
        DeviceAssignment assignment = ownedAssignment();
        assignment.putConfigurationValue("threshold.PM25", "{\"metric\":\"PM25\",\"value\":35.5,\"enabled\":true}");
        when(deviceAssignmentRepository.findByDeviceIdForUpdate(assignment.getDeviceId())).thenReturn(Optional.of(assignment));

        new DeviceThresholdCommandServiceImpl(deviceAssignmentRepository, objectMapper)
                .handle(new RemoveDeviceThresholdCommand(assignment.getDeviceId(), assignment.getOwnerUserId(), MetricThreshold.PM25));

        verify(deviceAssignmentRepository).save(assignment);
    }

    @Test
    void shouldThrowAccessDeniedWhenAssignmentBelongsToAnotherUser() {
        DeviceAssignment assignment = ownedAssignment();
        when(deviceAssignmentRepository.findByDeviceIdForUpdate(assignment.getDeviceId())).thenReturn(Optional.of(assignment));

        org.springframework.security.access.AccessDeniedException exception = assertThrowsExactly(
                org.springframework.security.access.AccessDeniedException.class,
                () -> new DeviceThresholdCommandServiceImpl(deviceAssignmentRepository, objectMapper)
                        .handle(new RemoveDeviceThresholdCommand(
                                assignment.getDeviceId(),
                                new UserId(UUID.randomUUID()),
                                MetricThreshold.PM25
                        ))
        );

        assertEquals("Device does not belong to user", exception.getMessage());
    }

    private DeviceAssignment ownedAssignment() {
        Device device = new Device(
                "SN-0200",
                "Sensor 0200",
                new HardwareId("CLAIR-0KBG"),
                ApiKey.generate(),
                new DeviceType("air-quality-v1")
        );
        org.springframework.test.util.ReflectionTestUtils.setField(device, "id", UUID.fromString("550e8400-e29b-41d4-a716-446655440800"));
        DeviceAssignment assignment = new DeviceAssignment(device.getId(), ClaimToken.generate());
        assignment.claimToSpace(UUID.fromString("550e8400-e29b-41d4-a716-446655440801"), new UserId(UUID.fromString("550e8400-e29b-41d4-a716-446655440802")));
        return assignment;
    }
}
