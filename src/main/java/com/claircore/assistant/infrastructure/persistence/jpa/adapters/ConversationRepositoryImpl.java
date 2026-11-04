package com.claircore.assistant.infrastructure.persistence.jpa.adapters;

import com.claircore.assistant.domain.model.aggregates.Conversation;
import com.claircore.assistant.domain.repositories.ConversationRepository;
import com.claircore.assistant.infrastructure.persistence.jpa.assemblers.ConversationPersistenceAssembler;
import com.claircore.assistant.infrastructure.persistence.jpa.repositories.ConversationPersistenceRepository;
import com.claircore.shared.domain.model.PageResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ConversationRepositoryImpl implements ConversationRepository {

    private static final Sort MOST_RECENT_ACTIVITY_FIRST = Sort.by(Sort.Direction.DESC, "lastMessageAt");

    private final ConversationPersistenceRepository conversationPersistenceRepository;

    public ConversationRepositoryImpl(ConversationPersistenceRepository conversationPersistenceRepository) {
        this.conversationPersistenceRepository = conversationPersistenceRepository;
    }

    @Override
    public Conversation save(Conversation conversation) {
        var saved = conversationPersistenceRepository.save(
                ConversationPersistenceAssembler.toPersistenceFromDomain(conversation));
        return ConversationPersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public Optional<Conversation> findByIdAndUserId(UUID conversationId, UUID userId) {
        return conversationPersistenceRepository.findByIdAndUserId(conversationId, userId)
                .map(ConversationPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public PageResult<Conversation> findByUserId(UUID userId, int page, int size) {
        var found = conversationPersistenceRepository.findByUserId(
                userId, PageRequest.of(page, size, MOST_RECENT_ACTIVITY_FIRST));
        return new PageResult<>(
                found.getContent().stream().map(ConversationPersistenceAssembler::toDomainFromPersistence).toList(),
                page,
                size,
                found.getTotalElements());
    }
}
