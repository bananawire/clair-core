package com.claircore.alerting.domain.model.queries;

import java.util.UUID;

public record GetAlertsByOwnerQuery(
        UUID ownerUserId,
        int page,
        int size
) {
    public GetAlertsByOwnerQuery {
        if (ownerUserId == null) {
            throw new IllegalArgumentException("Owner user ID must not be null");
        }
        if (page < 0) {
            throw new IllegalArgumentException("Page must not be negative");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }
    }
}
