package com.claircore.assistant.domain.repositories;

import com.claircore.assistant.domain.model.aggregates.Conversation;
import com.claircore.shared.domain.model.PageResult;

import java.util.Optional;
import java.util.UUID;

/** Port for conversation storage. Domain types only. */
public interface ConversationRepository {

    Conversation save(Conversation conversation);

    /** Empty when the conversation does not exist or belongs to someone else. */
    Optional<Conversation> findByIdAndUserId(UUID conversationId, UUID userId);

    /** Most recently active first; ordering is the adapter's responsibility. */
    PageResult<Conversation> findByUserId(UUID userId, int page, int size);
}
