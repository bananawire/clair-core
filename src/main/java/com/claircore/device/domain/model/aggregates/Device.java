package com.claircore.device.domain.model.aggregates;

import com.claircore.device.domain.model.valueobjects.ApiKey;
import com.claircore.device.domain.model.valueobjects.DeviceType;
import com.claircore.device.domain.model.valueobjects.HardwareId;

import java.time.Instant;
import java.util.UUID;

/**
 * One physical unit as the factory shipped it: its identifiers and its credential. Who owns it and
 * where it sits belong to {@link DeviceAssignment}, which is why a decommissioned device keeps its
 * row — the edge roster reads tombstones to learn a unit is gone.
 */
public class Device {

    private final UUID id;
    private final String serialNumber;
    private String name;
    private final String factoryName;
    private boolean deleted;
    private final HardwareId hardwareId;
    private ApiKey apiKey;
    private final DeviceType deviceType;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Device(UUID id, String serialNumber, String name, String factoryName, boolean deleted,
                   HardwareId hardwareId, ApiKey apiKey, DeviceType deviceType,
                   Instant createdAt, Instant updatedAt) {
        if (id == null) {
            throw new IllegalArgumentException("Id must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Device name must not be null or blank");
        }
        this.id = id;
        this.serialNumber = serialNumber;
        this.name = name;
        this.factoryName = factoryName;
        this.deleted = deleted;
        this.hardwareId = hardwareId;
        this.apiKey = apiKey;
        this.deviceType = deviceType;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Device(String serialNumber, String name, HardwareId hardwareId, ApiKey apiKey, DeviceType deviceType) {
        this(UUID.randomUUID(), serialNumber, name, name, false, hardwareId, apiKey, deviceType, null, null);
    }

    /** Rebuilds a device already in storage; only a persistence assembler should call this. */
    public static Device reconstitute(
            UUID id, String serialNumber, String name, String factoryName, boolean deleted,
            HardwareId hardwareId, ApiKey apiKey, DeviceType deviceType,
            Instant createdAt, Instant updatedAt) {
        return new Device(id, serialNumber, name, factoryName, deleted, hardwareId, apiKey, deviceType,
                createdAt, updatedAt);
    }

    public void rotateApiKey(ApiKey apiKey) {
        this.apiKey = apiKey;
    }

    public void updateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Device name must not be null or blank");
        }
        this.name = name;
    }

    public void resetNameToFactoryDefault() {
        this.name = this.factoryName;
    }

    /** Marks the device as decommissioned while retaining its row as a roster tombstone. */
    public void markDeleted() {
        this.deleted = true;
    }

    public boolean isDeleted() { return deleted; }

    public UUID getId() { return id; }
    public String getSerialNumber() { return serialNumber; }
    public String getName() { return name; }
    public String getFactoryName() { return factoryName; }
    public HardwareId getHardwareId() { return hardwareId; }
    public ApiKey getApiKey() { return apiKey; }
    public DeviceType getDeviceType() { return deviceType; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
