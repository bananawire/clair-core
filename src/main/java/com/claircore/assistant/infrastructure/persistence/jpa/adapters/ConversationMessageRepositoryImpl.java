package com.claircore.assistant.infrastructure.persistence.jpa.adapters;

import com.claircore.assistant.domain.model.aggregates.ConversationMessage;
import com.claircore.assistant.domain.model.valueobjects.MessageRole;
import com.claircore.assistant.domain.repositories.ConversationMessageRepository;
import com.claircore.assistant.infrastructure.persistence.jpa.assemblers.ConversationMessagePersistenceAssembler;
import com.claircore.assistant.infrastructure.persistence.jpa.repositories.ConversationMessagePersistenceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ConversationMessageRepositoryImpl implements ConversationMessageRepository {

    /*
     * A question and its answer are written in the same transaction, so their audit instants can
     * tie. Role breaks the tie: USER sorts after ASSISTANT alphabetically, hence DESC to put the
     * question first.
     */
    private static final Sort OLDEST_FIRST = Sort.by(Sort.Order.asc("createdAt"), Sort.Order.desc("role"));
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("role"));

    private final ConversationMessagePersistenceRepository conversationMessagePersistenceRepository;

    public ConversationMessageRepositoryImpl(ConversationMessagePersistenceRepository conversationMessagePersistenceRepository) {
        this.conversationMessagePersistenceRepository = conversationMessagePersistenceRepository;
    }

    @Override
    public ConversationMessage save(ConversationMessage message) {
        var saved = conversationMessagePersistenceRepository.save(
                ConversationMessagePersistenceAssembler.toPersistenceFromDomain(message));
        return ConversationMessagePersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public List<ConversationMessage> saveAll(List<ConversationMessage> messages) {
        return messages.stream().map(this::save).toList();
    }

    @Override
    public Optional<ConversationMessage> findById(UUID messageId) {
        return conversationMessagePersistenceRepository.findById(messageId)
                .map(ConversationMessagePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<ConversationMessage> findByConversationId(UUID conversationId) {
        return conversationMessagePersistenceRepository.findByConversationId(conversationId, OLDEST_FIRST).stream()
                .map(ConversationMessagePersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<ConversationMessage> findRecentByConversationId(UUID conversationId, int limit) {
        if (limit <= 0) return List.of();
        var newestFirst = conversationMessagePersistenceRepository
                .findByConversationId(conversationId, PageRequest.of(0, limit, NEWEST_FIRST)).stream()
                .map(ConversationMessagePersistenceAssembler::toDomainFromPersistence)
                .collect(ArrayList<ConversationMessage>::new, ArrayList::add, ArrayList::addAll);
        Collections.reverse(newestFirst);
        return List.copyOf(newestFirst);
    }

    @Override
    public long countByUserIdAndRoleSince(UUID userId, MessageRole role, Instant since) {
        return conversationMessagePersistenceRepository
                .countByUserIdAndRoleAndCreatedAtGreaterThanEqual(userId, role, since);
    }
}
