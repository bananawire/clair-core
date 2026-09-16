package com.claircore.alerting.domain.model.queries;

import java.util.UUID;

public record GetAlertsByDeviceQuery(
        UUID deviceId,
        int page,
        int size
) {
    public GetAlertsByDeviceQuery {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (page < 0) {
            throw new IllegalArgumentException("Page must not be negative");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }
    }
}
