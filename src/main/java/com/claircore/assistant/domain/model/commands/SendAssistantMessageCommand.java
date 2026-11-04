package com.claircore.assistant.domain.model.commands;

import com.claircore.assistant.domain.model.aggregates.ConversationMessage;
import com.claircore.assistant.domain.model.valueobjects.AssistantLocale;

import java.util.UUID;

/**
 * A question for the assistant. A null {@code conversationId} opens a new conversation.
 */
public record SendAssistantMessageCommand(UUID userId, UUID conversationId, String content, AssistantLocale locale) {
    public SendAssistantMessageCommand {
        if (userId == null) throw new IllegalArgumentException("User ID is required");
        if (content == null || content.isBlank()) throw new IllegalArgumentException("Content is required");
        content = content.strip();
        if (content.length() > ConversationMessage.CONTENT_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Content must be at most " + ConversationMessage.CONTENT_MAX_LENGTH + " characters");
        }
        if (locale == null) locale = AssistantLocale.ES;
    }
}
