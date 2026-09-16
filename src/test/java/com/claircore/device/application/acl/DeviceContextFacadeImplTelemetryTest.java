package com.claircore.device.application.acl;

import com.claircore.device.application.queryservices.DeviceQueryService;
import com.claircore.device.domain.model.aggregates.Device;
import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.queries.GetDevicesForTelemetryQuery;
import com.claircore.device.domain.model.valueobjects.ApiKey;
import com.claircore.device.domain.model.valueobjects.DeviceStatus;
import com.claircore.device.domain.model.valueobjects.DeviceType;
import com.claircore.device.domain.model.valueobjects.HardwareId;
import com.claircore.device.interfaces.acl.DeviceTelemetryTarget;
import com.claircore.shared.domain.model.PageResult;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceContextFacadeImplTelemetryTest {

    @Mock
    private DeviceQueryService deviceQueryService;

    @Test
    void findTelemetryTargetsMapsAssignedFlagAndBoundedLimit() {
        UUID deviceId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Device device = sampleDevice(deviceId);
        when(deviceQueryService.handle(any(GetDevicesForTelemetryQuery.class)))
                .thenReturn(new PageResult<>(List.of(device), 0, 5, 1));
        when(deviceQueryService.findAssignmentByDeviceId(deviceId))
                .thenReturn(Optional.of(sampleAssignment(deviceId)));

        var facade = new DeviceContextFacadeImpl(deviceQueryService);
        var targets = facade.findTelemetryTargets(5, false);

        assertThat(targets).hasSize(1);
        assertThat(targets.get(0).deviceId()).isEqualTo(deviceId);
        assertThat(targets.get(0).hardwareId()).isEqualTo("HW-0001");
        assertThat(targets.get(0).assigned()).isTrue();
        assertThat(targets.get(0).status()).isEqualTo("OFFLINE");
    }

    @Test
    void findTelemetryTargetsReportsAssignedFalseWhenAssignmentIsMissing() {
        UUID deviceId = UUID.randomUUID();
        when(deviceQueryService.handle(any(GetDevicesForTelemetryQuery.class)))
                .thenReturn(new PageResult<>(List.of(sampleDevice(deviceId)), 0, 5, 1));
        when(deviceQueryService.findAssignmentByDeviceId(deviceId)).thenReturn(Optional.empty());

        var facade = new DeviceContextFacadeImpl(deviceQueryService);
        var targets = facade.findTelemetryTargets(5, true);

        assertThat(targets).hasSize(1);
        assertThat(targets.get(0).assigned()).isFalse();
    }

    @Test
    void recordDevicePresenceRejectsUnknownStatus() {
        UUID deviceId = UUID.randomUUID();
        var facade = new DeviceContextFacadeImpl(deviceQueryService);
        assertThatThrownBy(() -> facade.recordDevicePresence(deviceId, "MAYBE", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recordDevicePresenceUpdatesTheAssignment() {
        UUID deviceId = UUID.randomUUID();
        Instant occurred = Instant.parse("2026-05-16T22:30:00Z");

        var facade = new DeviceContextFacadeImpl(deviceQueryService);
        facade.recordDevicePresence(deviceId, "ONLINE", occurred);

        verify(deviceQueryService).updatePresence(deviceId, DeviceStatus.ONLINE, occurred);
    }

    private static Device sampleDevice(UUID id) {
        Device device = new Device(
                "SN-0001",
                "Sensor 0001",
                new HardwareId("HW-0001"),
                ApiKey.generate(),
                new DeviceType("air-quality-v1"));
        org.springframework.test.util.ReflectionTestUtils.setField(device, "id", id);
        return device;
    }

    private static DeviceAssignment sampleAssignment(UUID deviceId) {
        DeviceAssignment assignment = new DeviceAssignment(deviceId,
                com.claircore.device.domain.model.valueobjects.ClaimToken.generate());
        return assignment;
    }
}
