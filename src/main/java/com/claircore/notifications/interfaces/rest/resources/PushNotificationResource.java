package com.claircore.notifications.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Push notification history item")
public record PushNotificationResource(
        @Schema(description = "Push notification log ID") UUID id,
        @Schema(description = "Recipient user ID") UUID userId,
        @Schema(description = "Related alert ID", nullable = true) UUID alertId,
        @Schema(description = "Notification title") String title,
        @Schema(description = "Notification message") String message,
        @Schema(description = "Delivery status", example = "SENT") String status,
        @Schema(description = "Delivery error message", nullable = true) String errorMessage,
        @Schema(description = "Creation timestamp") Instant createdAt
) {
}
