package com.claircore.assistant.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Assistant conversation history item")
public record ConversationResource(
        @Schema(description = "Conversation ID") UUID id,
        @Schema(description = "Start of the first question") String title,
        @Schema(description = "Last time a question was asked") Instant lastMessageAt,
        @Schema(description = "Creation timestamp") Instant createdAt
) {
}
