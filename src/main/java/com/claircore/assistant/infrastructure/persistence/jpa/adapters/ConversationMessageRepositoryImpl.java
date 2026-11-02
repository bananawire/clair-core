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

    /* Turns written in one transaction can share an audit instant, so order is the stored position. */
    private static final Sort FIRST_TO_LAST = Sort.by(Sort.Direction.ASC, "position");
    private static final Sort LAST_TO_FIRST = Sort.by(Sort.Direction.DESC, "position");

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
        return conversationMessagePersistenceRepository.findByConversationId(conversationId, FIRST_TO_LAST).stream()
                .map(ConversationMessagePersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<ConversationMessage> findRecentByConversationId(UUID conversationId, int limit) {
        if (limit <= 0) return List.of();
        var lastFirst = conversationMessagePersistenceRepository
                .findByConversationId(conversationId, PageRequest.of(0, limit, LAST_TO_FIRST)).stream()
                .map(ConversationMessagePersistenceAssembler::toDomainFromPersistence)
                .collect(ArrayList<ConversationMessage>::new, ArrayList::add, ArrayList::addAll);
        Collections.reverse(lastFirst);
        return List.copyOf(lastFirst);
    }

    @Override
    public long countByUserIdAndRoleSince(UUID userId, MessageRole role, Instant since) {
        return conversationMessagePersistenceRepository
                .countByUserIdAndRoleAndCreatedAtGreaterThanEqual(userId, role, since);
    }
}
