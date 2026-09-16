package com.claircore.notifications.domain.model.queries;

import java.util.UUID;

public record GetPushNotificationHistoryQuery(UUID userId, int page, int size) {
    public GetPushNotificationHistoryQuery {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (page < 0) {
            throw new IllegalArgumentException("Page must not be negative");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }
    }
}
