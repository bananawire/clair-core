package com.claircore.assistant.infrastructure.persistence.jpa.assemblers;

import com.claircore.assistant.domain.model.aggregates.Conversation;
import com.claircore.assistant.infrastructure.persistence.jpa.entities.ConversationPersistenceEntity;

public final class ConversationPersistenceAssembler {

    private ConversationPersistenceAssembler() {
    }

    public static Conversation toDomainFromPersistence(ConversationPersistenceEntity entity) {
        if (entity == null) return null;
        return Conversation.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getTitle(),
                entity.getLastMessageAt(),
                entity.getMessageCount(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static ConversationPersistenceEntity toPersistenceFromDomain(Conversation conversation) {
        if (conversation == null) return null;
        var entity = new ConversationPersistenceEntity();
        entity.setId(conversation.getId());
        entity.setUserId(conversation.getUserId());
        entity.setTitle(conversation.getTitle());
        entity.setLastMessageAt(conversation.getLastMessageAt());
        entity.setMessageCount(conversation.getMessageCount());
        // Null timestamps are what mark the entity as new (see AuditableAbstractPersistenceEntity).
        entity.setCreatedAt(conversation.getCreatedAt());
        entity.setUpdatedAt(conversation.getUpdatedAt());
        return entity;
    }
}
