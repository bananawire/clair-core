package com.claircore.alerting.domain.model.queries;

import java.util.UUID;

public record GetAlertsBySpaceQuery(
        UUID spaceId,
        int page,
        int size
) {
    public GetAlertsBySpaceQuery {
        if (spaceId == null) {
            throw new IllegalArgumentException("Space ID must not be null");
        }
        if (page < 0) {
            throw new IllegalArgumentException("Page must not be negative");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }
    }
}
