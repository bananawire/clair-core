package com.claircore.assistant.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "The stored question, the assistant answer and the allowance left")
public record AssistantReplyResource(
        @Schema(description = "Conversation ID, new or continued") UUID conversationId,
        @Schema(description = "The user's question as stored") ConversationMessageResource userMessage,
        @Schema(description = "The assistant answer") ConversationMessageResource assistantMessage,
        @Schema(description = "Allowance after this question") AssistantUsageResource usage
) {
}
