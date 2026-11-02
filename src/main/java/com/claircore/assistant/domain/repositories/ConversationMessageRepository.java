package com.claircore.assistant.domain.repositories;

import com.claircore.assistant.domain.model.aggregates.ConversationMessage;
import com.claircore.assistant.domain.model.valueobjects.MessageRole;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port for conversation message storage. Domain types only. */
public interface ConversationMessageRepository {

    ConversationMessage save(ConversationMessage message);

    /** Stores the messages in the given order, which is also the order they are read back in. */
    List<ConversationMessage> saveAll(List<ConversationMessage> messages);

    Optional<ConversationMessage> findById(UUID messageId);

    /** Oldest first: the order the conversation is displayed in. */
    List<ConversationMessage> findByConversationId(UUID conversationId);

    /** The newest {@code limit} messages of a conversation, returned oldest first. */
    List<ConversationMessage> findRecentByConversationId(UUID conversationId, int limit);

    /** How many messages of {@code role} the user wrote at or after {@code since}. */
    long countByUserIdAndRoleSince(UUID userId, MessageRole role, Instant since);
}
