package com.claircore.assistant.domain.model.queries;

import java.util.UUID;

public record GetAssistantUsageQuery(UUID userId) {
    public GetAssistantUsageQuery {
        if (userId == null) throw new IllegalArgumentException("User ID is required");
    }
}
