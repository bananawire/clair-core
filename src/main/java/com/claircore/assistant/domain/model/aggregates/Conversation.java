package com.claircore.assistant.domain.model.aggregates;

import java.time.Instant;
import java.util.UUID;

/**
 * A thread of messages between one user and the assistant. Its title is the start of the first
 * question, so the list of conversations reads like a history without asking the model for one.
 *
 * <p>It hands out the position of every turn: a question and its answer are stored in the same
 * transaction and can share a timestamp, so the order of a conversation must not depend on the clock.
 */
public class Conversation {

    public static final int TITLE_MAX_LENGTH = 60;

    private final UUID id;
    private final UUID userId;
    private final String title;
    private Instant lastMessageAt;
    private int messageCount;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Conversation(UUID id, UUID userId, String title, Instant lastMessageAt, int messageCount,
                         Instant createdAt, Instant updatedAt) {
        if (id == null) throw new IllegalArgumentException("Id is required");
        if (userId == null) throw new IllegalArgumentException("User ID is required");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Title is required");
        if (lastMessageAt == null) throw new IllegalArgumentException("Last message at is required");
        if (messageCount < 0) throw new IllegalArgumentException("Message count must not be negative");
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.lastMessageAt = lastMessageAt;
        this.messageCount = messageCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Opens a conversation titled after the first question the user asks in it. */
    public static Conversation start(UUID userId, String firstMessage, Instant startedAt) {
        if (firstMessage == null || firstMessage.isBlank()) {
            throw new IllegalArgumentException("First message is required");
        }
        return new Conversation(UUID.randomUUID(), userId, titleFrom(firstMessage), startedAt, 0, null, null);
    }

    /** Rebuilds a conversation that already exists in storage, identity and audit timestamps included. */
    public static Conversation reconstitute(UUID id, UUID userId, String title, Instant lastMessageAt,
                                            int messageCount, Instant createdAt, Instant updatedAt) {
        return new Conversation(id, userId, title, lastMessageAt, messageCount, createdAt, updatedAt);
    }

    /**
     * Reserves the positions of one question and its answer and moves the conversation to the top
     * of the user's history.
     *
     * @return the position of the question; the answer takes the next one
     */
    public int recordExchange(Instant at) {
        if (at == null) throw new IllegalArgumentException("Exchange instant is required");
        if (at.isAfter(lastMessageAt)) {
            this.lastMessageAt = at;
        }
        int questionPosition = messageCount;
        this.messageCount += 2;
        return questionPosition;
    }

    public boolean belongsTo(UUID candidateUserId) {
        return userId.equals(candidateUserId);
    }

    private static String titleFrom(String firstMessage) {
        String singleLine = firstMessage.strip().replaceAll("\\s+", " ");
        return singleLine.length() <= TITLE_MAX_LENGTH
                ? singleLine
                : singleLine.substring(0, TITLE_MAX_LENGTH - 3).stripTrailing() + "...";
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTitle() { return title; }
    public Instant getLastMessageAt() { return lastMessageAt; }
    public int getMessageCount() { return messageCount; }

    /** Null until the conversation has been written; assigned by persistence auditing. */
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
