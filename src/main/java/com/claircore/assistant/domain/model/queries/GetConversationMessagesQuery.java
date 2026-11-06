package com.claircore.assistant.domain.model.queries;

import java.util.UUID;

public record GetConversationMessagesQuery(UUID userId, UUID conversationId) {
    public GetConversationMessagesQuery {
        if (userId == null) throw new IllegalArgumentException("User ID is required");
        if (conversationId == null) throw new IllegalArgumentException("Conversation ID is required");
    }
}
