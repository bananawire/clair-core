package com.claircore.assistant.domain.model.aggregates;

import com.claircore.assistant.domain.model.valueobjects.FeedbackRating;
import com.claircore.assistant.domain.model.valueobjects.MessageRole;

import java.time.Instant;
import java.util.UUID;

/**
 * One turn of a conversation. Assistant turns also keep the model, token counts and latency of the
 * call that produced them, and are the only turns the user can rate.
 */
public class ConversationMessage {

    public static final int CONTENT_MAX_LENGTH = 1000;

    private final UUID id;
    private final UUID conversationId;
    private final UUID userId;
    private final MessageRole role;
    private final String content;
    private final String model;
    private final Integer promptTokens;
    private final Integer completionTokens;
    private final Integer latencyMs;
    private FeedbackRating feedback;
    private final Instant createdAt;
    private final Instant updatedAt;

    private ConversationMessage(
            UUID id,
            UUID conversationId,
            UUID userId,
            MessageRole role,
            String content,
            String model,
            Integer promptTokens,
            Integer completionTokens,
            Integer latencyMs,
            FeedbackRating feedback,
            Instant createdAt,
            Instant updatedAt) {
        if (id == null) throw new IllegalArgumentException("Id is required");
        if (conversationId == null) throw new IllegalArgumentException("Conversation ID is required");
        if (userId == null) throw new IllegalArgumentException("User ID is required");
        if (role == null) throw new IllegalArgumentException("Role is required");
        if (content == null || content.isBlank()) throw new IllegalArgumentException("Content is required");
        if (role == MessageRole.USER && content.length() > CONTENT_MAX_LENGTH) {
            throw new IllegalArgumentException("Content must be at most " + CONTENT_MAX_LENGTH + " characters");
        }
        if (role == MessageRole.USER && feedback != null) {
            throw new IllegalArgumentException("Only assistant messages can be rated");
        }
        this.id = id;
        this.conversationId = conversationId;
        this.userId = userId;
        this.role = role;
        this.content = content;
        this.model = model;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.latencyMs = latencyMs;
        this.feedback = feedback;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ConversationMessage user(UUID conversationId, UUID userId, String content) {
        return new ConversationMessage(UUID.randomUUID(), conversationId, userId, MessageRole.USER,
                content == null ? null : content.strip(), null, null, null, null, null, null, null);
    }

    public static ConversationMessage assistant(UUID conversationId, UUID userId, String content, String model,
                                                Integer promptTokens, Integer completionTokens, Integer latencyMs) {
        return new ConversationMessage(UUID.randomUUID(), conversationId, userId, MessageRole.ASSISTANT,
                content == null ? null : content.strip(), model, promptTokens, completionTokens, latencyMs,
                null, null, null);
    }

    /** Rebuilds a message that already exists in storage, identity and audit timestamps included. */
    public static ConversationMessage reconstitute(
            UUID id,
            UUID conversationId,
            UUID userId,
            MessageRole role,
            String content,
            String model,
            Integer promptTokens,
            Integer completionTokens,
            Integer latencyMs,
            FeedbackRating feedback,
            Instant createdAt,
            Instant updatedAt) {
        return new ConversationMessage(id, conversationId, userId, role, content, model, promptTokens,
                completionTokens, latencyMs, feedback, createdAt, updatedAt);
    }

    /** Records the user's verdict; rating again replaces the previous one. */
    public void rate(FeedbackRating rating) {
        if (rating == null) throw new IllegalArgumentException("Rating is required");
        if (role != MessageRole.ASSISTANT) throw new IllegalStateException("Only assistant messages can be rated");
        this.feedback = rating;
    }

    public boolean isFromAssistant() {
        return role == MessageRole.ASSISTANT;
    }

    public boolean belongsTo(UUID candidateUserId) {
        return userId.equals(candidateUserId);
    }

    public UUID getId() { return id; }
    public UUID getConversationId() { return conversationId; }
    public UUID getUserId() { return userId; }
    public MessageRole getRole() { return role; }
    public String getContent() { return content; }
    public String getModel() { return model; }
    public Integer getPromptTokens() { return promptTokens; }
    public Integer getCompletionTokens() { return completionTokens; }
    public Integer getLatencyMs() { return latencyMs; }
    public FeedbackRating getFeedback() { return feedback; }

    /** Null until the message has been written; assigned by persistence auditing. */
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
