package com.claircore.device.infrastructure.persistence.jpa.entities;

import com.claircore.device.domain.model.valueobjects.ApiKey;
import com.claircore.device.domain.model.valueobjects.DeviceType;
import com.claircore.device.domain.model.valueobjects.HardwareId;
import com.claircore.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** Storage shape of {@code Device}. */
@Entity
@Table(name = "devices", indexes = @Index(name = "idx_devices_updated_at", columnList = "updated_at"))
public class DevicePersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "serial_number", nullable = false, unique = true)
    private String serialNumber;

    @Column(nullable = false)
    private String name;

    @Column(name = "factory_name", nullable = false)
    private String factoryName;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "hardware_id", nullable = false, unique = true)
    private HardwareId hardwareId;

    @Column(name = "api_key", nullable = false, unique = true, length = 255)
    private ApiKey apiKey;

    @Column(name = "device_type", nullable = false)
    private DeviceType deviceType;

    public DevicePersistenceEntity() {
        // JPA, and the persistence assembler
    }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFactoryName() { return factoryName; }
    public void setFactoryName(String factoryName) { this.factoryName = factoryName; }

    /** Legacy rows predate the column and hold NULL; those devices are active, not deleted. */
    public boolean isDeleted() { return Boolean.TRUE.equals(deleted); }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public HardwareId getHardwareId() { return hardwareId; }
    public void setHardwareId(HardwareId hardwareId) { this.hardwareId = hardwareId; }

    public ApiKey getApiKey() { return apiKey; }
    public void setApiKey(ApiKey apiKey) { this.apiKey = apiKey; }

    public DeviceType getDeviceType() { return deviceType; }
    public void setDeviceType(DeviceType deviceType) { this.deviceType = deviceType; }
}
