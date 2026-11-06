package com.claircore.assistant.infrastructure.persistence.jpa.repositories;

import com.claircore.assistant.infrastructure.persistence.jpa.entities.ConversationPersistenceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationPersistenceRepository extends JpaRepository<ConversationPersistenceEntity, UUID> {
    Optional<ConversationPersistenceEntity> findByIdAndUserId(UUID id, UUID userId);

    Page<ConversationPersistenceEntity> findByUserId(UUID userId, Pageable pageable);
}
