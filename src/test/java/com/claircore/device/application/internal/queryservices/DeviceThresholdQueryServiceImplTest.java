package com.claircore.device.application.internal.queryservices;

import com.claircore.device.domain.model.aggregates.Device;
import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.queries.GetDeviceThresholdByMetricQuery;
import com.claircore.device.domain.model.queries.GetDeviceThresholdsByDeviceQuery;
import com.claircore.device.domain.model.valueobjects.*;
import com.claircore.device.domain.repositories.DeviceAssignmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceThresholdQueryServiceImplTest {

    @Mock
    private DeviceAssignmentRepository deviceAssignmentRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReturnThresholdsWhenDeviceBelongsToUser() throws Exception {
        DeviceAssignment assignment = ownedAssignment();
        assignment.putConfigurationValue("threshold.PM25", objectMapper.writeValueAsString(new DeviceMetricThresholdConfiguration(MetricThreshold.PM25, new BigDecimal("35.5"), true)));
        when(deviceAssignmentRepository.findByDeviceId(assignment.getDeviceId())).thenReturn(Optional.of(assignment));

        List<DeviceMetricThresholdConfiguration> result = new DeviceThresholdQueryServiceImpl(deviceAssignmentRepository, objectMapper)
                .handle(new GetDeviceThresholdsByDeviceQuery(assignment.getDeviceId(), assignment.getOwnerUserId()));

        assertEquals(1, result.size());
    }

    @Test
    void shouldReturnThresholdByMetricWhenConfigurationExists() throws Exception {
        DeviceAssignment assignment = ownedAssignment();
        assignment.putConfigurationValue("threshold.PM25", objectMapper.writeValueAsString(new DeviceMetricThresholdConfiguration(MetricThreshold.PM25, new BigDecimal("35.5"), true)));
        when(deviceAssignmentRepository.findById(UUID.fromString("550e8400-e29b-41d4-a716-446655441200"))).thenReturn(Optional.of(assignment));

        Optional<DeviceMetricThresholdConfiguration> result = new DeviceThresholdQueryServiceImpl(deviceAssignmentRepository, objectMapper)
                .handle(new GetDeviceThresholdByMetricQuery(UUID.fromString("550e8400-e29b-41d4-a716-446655441200"), MetricThreshold.PM25));

        assertEquals(true, result.isPresent());
    }

    @Test
    void shouldThrowAccessDeniedWhenDeviceDoesNotBelongToUser() {
        DeviceAssignment assignment = ownedAssignment();
        when(deviceAssignmentRepository.findByDeviceId(assignment.getDeviceId())).thenReturn(Optional.of(assignment));

        assertThrowsExactly(
                org.springframework.security.access.AccessDeniedException.class,
                () -> new DeviceThresholdQueryServiceImpl(deviceAssignmentRepository, objectMapper)
                        .handle(new GetDeviceThresholdsByDeviceQuery(assignment.getDeviceId(), new UserId(UUID.randomUUID())))
        );
    }

    private DeviceAssignment ownedAssignment() {
        Device device = new Device("SN-0600", "Sensor 0600", new HardwareId("CLAIR-0KBG"), ApiKey.generate(), new DeviceType("air-quality-v1"));
        org.springframework.test.util.ReflectionTestUtils.setField(device, "id", UUID.fromString("550e8400-e29b-41d4-a716-446655441201"));
        DeviceAssignment assignment = new DeviceAssignment(device.getId(), ClaimToken.generate());
        assignment.claimToSpace(UUID.randomUUID(), new UserId(UUID.fromString("550e8400-e29b-41d4-a716-446655441202")));
        return assignment;
    }
}
