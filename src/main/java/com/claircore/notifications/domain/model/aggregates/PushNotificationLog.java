package com.claircore.notifications.domain.model.aggregates;

import java.time.Instant;
import java.util.UUID;

/**
 * Record of one push notification delivery attempt, successful or failed. Append-only, like
 * {@link EmailLog}.
 */
public class PushNotificationLog {

    private final UUID id;
    private final UUID userId;
    private final UUID alertId;
    private final String title;
    private final String message;
    private final boolean sent;
    private final String errorMessage;
    private final Instant createdAt;
    private final Instant updatedAt;

    private PushNotificationLog(
            UUID id,
            UUID userId,
            UUID alertId,
            String title,
            String message,
            boolean sent,
            String errorMessage,
            Instant createdAt,
            Instant updatedAt) {
        if (id == null) throw new IllegalArgumentException("Id is required");
        if (userId == null) throw new IllegalArgumentException("User ID is required");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Title is required");
        if (message == null || message.isBlank()) throw new IllegalArgumentException("Message is required");
        this.id = id;
        this.userId = userId;
        this.alertId = alertId;
        this.title = title;
        this.message = message;
        this.sent = sent;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public PushNotificationLog(UUID userId, UUID alertId, String title, String message, boolean sent, String errorMessage) {
        this(UUID.randomUUID(), userId, alertId, title, message, sent, errorMessage, null, null);
    }

    public static PushNotificationLog sent(UUID userId, UUID alertId, String title, String message) {
        return new PushNotificationLog(userId, alertId, title, message, true, null);
    }

    public static PushNotificationLog failed(UUID userId, UUID alertId, String title, String message, String errorMessage) {
        return new PushNotificationLog(userId, alertId, title, message, false, errorMessage);
    }

    /** Rebuilds a log that already exists in storage, identity and audit timestamps included. */
    public static PushNotificationLog reconstitute(
            UUID id,
            UUID userId,
            UUID alertId,
            String title,
            String message,
            boolean sent,
            String errorMessage,
            Instant createdAt,
            Instant updatedAt) {
        return new PushNotificationLog(id, userId, alertId, title, message, sent, errorMessage, createdAt, updatedAt);
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getAlertId() { return alertId; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public boolean isSent() { return sent; }
    public String getErrorMessage() { return errorMessage; }

    /** Null until the log has been written; assigned by persistence auditing. */
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
