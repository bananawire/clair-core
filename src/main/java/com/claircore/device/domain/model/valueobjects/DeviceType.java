package com.claircore.device.domain.model.valueobjects;

public record DeviceType(String value) {
    public DeviceType {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Device type must not be null or blank");
        }
    }
}
