package com.claircore.assistant.infrastructure.persistence.jpa.assemblers;

import com.claircore.assistant.domain.model.aggregates.ConversationMessage;
import com.claircore.assistant.infrastructure.persistence.jpa.entities.ConversationMessagePersistenceEntity;

public final class ConversationMessagePersistenceAssembler {

    private ConversationMessagePersistenceAssembler() {
    }

    public static ConversationMessage toDomainFromPersistence(ConversationMessagePersistenceEntity entity) {
        if (entity == null) return null;
        return ConversationMessage.reconstitute(
                entity.getId(),
                entity.getConversationId(),
                entity.getUserId(),
                entity.getRole(),
                entity.getContent(),
                entity.getModel(),
                entity.getPromptTokens(),
                entity.getCompletionTokens(),
                entity.getLatencyMs(),
                entity.getFeedback(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static ConversationMessagePersistenceEntity toPersistenceFromDomain(ConversationMessage message) {
        if (message == null) return null;
        var entity = new ConversationMessagePersistenceEntity();
        entity.setId(message.getId());
        entity.setConversationId(message.getConversationId());
        entity.setUserId(message.getUserId());
        entity.setRole(message.getRole());
        entity.setContent(message.getContent());
        entity.setModel(message.getModel());
        entity.setPromptTokens(message.getPromptTokens());
        entity.setCompletionTokens(message.getCompletionTokens());
        entity.setLatencyMs(message.getLatencyMs());
        entity.setFeedback(message.getFeedback());
        // Null timestamps are what mark the entity as new (see AuditableAbstractPersistenceEntity).
        entity.setCreatedAt(message.getCreatedAt());
        entity.setUpdatedAt(message.getUpdatedAt());
        return entity;
    }
}
