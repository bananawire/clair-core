package com.claircore.evaluation.domain.model.valueobjects;

import java.util.UUID;

public record DeviceId(UUID value) {
    public DeviceId {
        if (value == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
    }
}
