package com.claircore.assistant.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "One turn of an assistant conversation")
public record ConversationMessageResource(
        @Schema(description = "Message ID") UUID id,
        @Schema(description = "Conversation ID") UUID conversationId,
        @Schema(description = "Author", example = "ASSISTANT", allowableValues = {"USER", "ASSISTANT"}) String role,
        @Schema(description = "Message text") String content,
        @Schema(description = "User verdict on an assistant answer", nullable = true, allowableValues = {"UP", "DOWN"}) String feedback,
        @Schema(description = "Creation timestamp") Instant createdAt
) {
}
