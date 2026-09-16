package com.claircore.notifications.infrastructure.persistence.jpa.adapters;

import com.claircore.notifications.domain.model.aggregates.EmailLog;
import com.claircore.notifications.domain.model.valueobjects.EmailRecipient;
import com.claircore.notifications.domain.repositories.EmailLogRepository;
import com.claircore.notifications.infrastructure.persistence.jpa.assemblers.EmailLogPersistenceAssembler;
import com.claircore.notifications.infrastructure.persistence.jpa.repositories.EmailLogPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EmailLogRepositoryImpl implements EmailLogRepository {

    private final EmailLogPersistenceRepository emailLogPersistenceRepository;

    public EmailLogRepositoryImpl(EmailLogPersistenceRepository emailLogPersistenceRepository) {
        this.emailLogPersistenceRepository = emailLogPersistenceRepository;
    }

    @Override
    public EmailLog save(EmailLog emailLog) {
        var saved = emailLogPersistenceRepository.save(EmailLogPersistenceAssembler.toPersistenceFromDomain(emailLog));
        return EmailLogPersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public List<EmailLog> findByRecipient(EmailRecipient recipient) {
        return emailLogPersistenceRepository.findByRecipientEmail(recipient).stream()
                .map(EmailLogPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }
}
