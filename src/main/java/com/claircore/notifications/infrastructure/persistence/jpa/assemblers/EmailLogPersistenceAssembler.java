package com.claircore.notifications.infrastructure.persistence.jpa.assemblers;

import com.claircore.notifications.domain.model.aggregates.EmailLog;
import com.claircore.notifications.infrastructure.persistence.jpa.entities.EmailLogPersistenceEntity;

public final class EmailLogPersistenceAssembler {

    private EmailLogPersistenceAssembler() {
    }

    public static EmailLog toDomainFromPersistence(EmailLogPersistenceEntity entity) {
        if (entity == null) return null;
        return EmailLog.reconstitute(
                entity.getId(),
                entity.getRecipientEmail(),
                entity.getSubject(),
                entity.getContent(),
                entity.isSent(),
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static EmailLogPersistenceEntity toPersistenceFromDomain(EmailLog emailLog) {
        if (emailLog == null) return null;
        var entity = new EmailLogPersistenceEntity();
        entity.setId(emailLog.getId());
        entity.setRecipientEmail(emailLog.getRecipientEmail());
        entity.setSubject(emailLog.getSubject());
        entity.setContent(emailLog.getContent());
        entity.setSent(emailLog.isSent());
        entity.setErrorMessage(emailLog.getErrorMessage());
        // Null for a log that has never been written; @CreatedDate fills it on insert, and leaving
        // it null is what marks the entity as new. See AuditableAbstractPersistenceEntity#isNew.
        entity.setCreatedAt(emailLog.getCreatedAt());
        entity.setUpdatedAt(emailLog.getUpdatedAt());
        return entity;
    }
}
