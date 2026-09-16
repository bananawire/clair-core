package com.claircore.device.domain.model.aggregates;

import com.claircore.device.domain.model.valueobjects.ApiKey;
import com.claircore.device.domain.model.valueobjects.DeviceType;
import com.claircore.device.domain.model.valueobjects.HardwareId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class DeviceTest {

    @Test
    void shouldCreateDeviceWhenArgumentsAreValid() {
        Device device = new Device(
                "SN-0001",
                "Sensor 0001",
                new HardwareId("CLAIR-0KBG"),
                ApiKey.generate(),
                new DeviceType("air-quality-v1")
        );

        assertEquals("SN-0001", device.getSerialNumber());
        assertEquals("Sensor 0001", device.getName());
        assertEquals("Sensor 0001", device.getFactoryName());
    }

    @Test
    void shouldRejectBlankNameWhenUpdatingDeviceName() {
        Device device = new Device(
                "SN-0001",
                "Sensor 0001",
                new HardwareId("CLAIR-0KBG"),
                ApiKey.generate(),
                new DeviceType("air-quality-v1")
        );

        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> device.updateName(" ")
        );

        assertEquals("Device name must not be null or blank", exception.getMessage());
    }

    @Test
    void shouldMarkDeletedWithoutRemovingTheDevice() {
        Device device = new Device("SN-0001", "Sensor 0001", new HardwareId("CLAIR-0KBG"), ApiKey.generate(), new DeviceType("air-quality-v1"));

        device.markDeleted();

        assertEquals(true, device.isDeleted());
    }

    @Test
    void shouldNotBeDeletedByDefault() {
        Device device = new Device("SN-0001", "Sensor 0001", new HardwareId("CLAIR-0KBG"), ApiKey.generate(), new DeviceType("air-quality-v1"));

        assertEquals(false, device.isDeleted());
    }

    @Test
    void carriesNoTimestampsUntilItHasBeenStored() {
        Device device = new Device("SN-0001", "Sensor 0001", new HardwareId("CLAIR-0KBG"),
                ApiKey.generate(), new DeviceType("air-quality-v1"));

        // The aggregate used to force updatedAt forward itself. Auditing does that now, on the
        // update the mutation causes; the roster cursor behaviour is asserted against storage in
        // DeviceRepositoryImplTest.
        assertEquals(null, device.getCreatedAt());
        assertEquals(null, device.getUpdatedAt());
    }

    @Test
    void shouldResetNameToFactoryDefaultWhenRequested() {
        Device device = new Device(
                "SN-0001",
                "Sensor 0001",
                new HardwareId("CLAIR-0KBG"),
                ApiKey.generate(),
                new DeviceType("air-quality-v1")
        );

        device.updateName("Kitchen sensor");
        device.resetNameToFactoryDefault();

        assertEquals("Sensor 0001", device.getName());
    }
}
