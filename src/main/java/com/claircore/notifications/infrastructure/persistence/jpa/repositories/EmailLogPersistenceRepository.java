package com.claircore.notifications.infrastructure.persistence.jpa.repositories;

import com.claircore.notifications.domain.model.valueobjects.EmailRecipient;
import com.claircore.notifications.infrastructure.persistence.jpa.entities.EmailLogPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmailLogPersistenceRepository extends JpaRepository<EmailLogPersistenceEntity, UUID> {
    List<EmailLogPersistenceEntity> findByRecipientEmail(EmailRecipient recipientEmail);
}
