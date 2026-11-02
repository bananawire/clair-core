package com.claircore.assistant.application.commandservices;

import com.claircore.assistant.domain.model.aggregates.ConversationMessage;
import com.claircore.assistant.domain.model.valueobjects.DailyUsage;

import java.util.UUID;

/** Outcome of one question: both stored turns and the allowance left afterwards. */
public record AssistantReply(UUID conversationId, ConversationMessage userMessage,
                             ConversationMessage assistantMessage, DailyUsage usage) {
}
