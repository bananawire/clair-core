package com.claircore.notifications.domain.model.commands;

import java.util.UUID;

public record SendPushNotificationCommand(UUID userId, UUID alertId, String title, String message) {
    public SendPushNotificationCommand {
        if (userId == null) throw new IllegalArgumentException("User ID is required");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Title is required");
        if (message == null || message.isBlank()) throw new IllegalArgumentException("Message is required");
    }
}
