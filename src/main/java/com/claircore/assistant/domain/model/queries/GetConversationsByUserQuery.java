package com.claircore.assistant.domain.model.queries;

import java.util.UUID;

public record GetConversationsByUserQuery(UUID userId, int page, int size) {
    public GetConversationsByUserQuery {
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
