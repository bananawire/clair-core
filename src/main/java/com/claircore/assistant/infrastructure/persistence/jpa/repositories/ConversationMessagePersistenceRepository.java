package com.claircore.assistant.infrastructure.persistence.jpa.repositories;

import com.claircore.assistant.domain.model.valueobjects.MessageRole;
import com.claircore.assistant.infrastructure.persistence.jpa.entities.ConversationMessagePersistenceEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ConversationMessagePersistenceRepository extends JpaRepository<ConversationMessagePersistenceEntity, UUID> {
    List<ConversationMessagePersistenceEntity> findByConversationId(UUID conversationId, Sort sort);

    List<ConversationMessagePersistenceEntity> findByConversationId(UUID conversationId, Pageable pageable);

    long countByUserIdAndRoleAndCreatedAtGreaterThanEqual(UUID userId, MessageRole role, Instant since);
}
