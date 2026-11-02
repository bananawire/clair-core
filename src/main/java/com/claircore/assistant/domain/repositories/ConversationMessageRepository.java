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

    /** Stores the messages; they are read back by position, not by insertion order. */
    List<ConversationMessage> saveAll(List<ConversationMessage> messages);

    Optional<ConversationMessage> findById(UUID messageId);

    /** By position: the order the conversation is displayed in. */
    List<ConversationMessage> findByConversationId(UUID conversationId);

    /** The last {@code limit} messages of a conversation, returned by position. */
    List<ConversationMessage> findRecentByConversationId(UUID conversationId, int limit);

    /** How many messages of {@code role} the user wrote at or after {@code since}. */
    long countByUserIdAndRoleSince(UUID userId, MessageRole role, Instant since);
}
