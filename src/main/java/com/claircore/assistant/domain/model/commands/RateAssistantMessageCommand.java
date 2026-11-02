package com.claircore.assistant.domain.model.commands;

import com.claircore.assistant.domain.model.valueobjects.FeedbackRating;

import java.util.UUID;

public record RateAssistantMessageCommand(UUID userId, UUID messageId, FeedbackRating rating) {
    public RateAssistantMessageCommand {
        if (userId == null) throw new IllegalArgumentException("User ID is required");
        if (messageId == null) throw new IllegalArgumentException("Message ID is required");
        if (rating == null) throw new IllegalArgumentException("Rating is required");
    }
}
