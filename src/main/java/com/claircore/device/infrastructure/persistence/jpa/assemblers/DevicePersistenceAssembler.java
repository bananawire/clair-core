package com.claircore.device.infrastructure.persistence.jpa.assemblers;

import com.claircore.device.domain.model.aggregates.Device;
import com.claircore.device.infrastructure.persistence.jpa.entities.DevicePersistenceEntity;

public final class DevicePersistenceAssembler {

    private DevicePersistenceAssembler() {
    }

    public static Device toDomainFromPersistence(DevicePersistenceEntity entity) {
        if (entity == null) return null;
        return Device.reconstitute(
                entity.getId(),
                entity.getSerialNumber(),
                entity.getName(),
                entity.getFactoryName(),
                entity.isDeleted(),
                entity.getHardwareId(),
                entity.getApiKey(),
                entity.getDeviceType(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static DevicePersistenceEntity toPersistenceFromDomain(Device device) {
        if (device == null) return null;
        var entity = new DevicePersistenceEntity();
        entity.setId(device.getId());
        entity.setSerialNumber(device.getSerialNumber());
        entity.setName(device.getName());
        entity.setFactoryName(device.getFactoryName());
        entity.setDeleted(device.isDeleted());
        entity.setHardwareId(device.getHardwareId());
        entity.setApiKey(device.getApiKey());
        entity.setDeviceType(device.getDeviceType());
        // Null for a device that has never been written; that is what marks the entity as new.
        entity.setCreatedAt(device.getCreatedAt());
        entity.setUpdatedAt(device.getUpdatedAt());
        return entity;
    }
}
