package com.claircore.notifications.infrastructure.persistence.jpa.entities;

import com.claircore.notifications.domain.model.valueobjects.EmailContent;
import com.claircore.notifications.domain.model.valueobjects.EmailRecipient;
import com.claircore.notifications.domain.model.valueobjects.EmailSubject;
import com.claircore.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Storage shape of {@code EmailLog}. Column definitions are the ones the aggregate carried before
 * the split, unchanged; the value objects reach their columns through the converters in
 * {@code infrastructure/persistence/jpa/converters}.
 */
@Entity
@Table(name = "email_logs")
public class EmailLogPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "recipient_email", nullable = false)
    private EmailRecipient recipientEmail;

    @Column(name = "subject", nullable = false)
    private EmailSubject subject;

    @Column(name = "content", columnDefinition = "TEXT")
    private EmailContent content;

    @Column(nullable = false)
    private boolean sent;

    private String errorMessage;

    public EmailLogPersistenceEntity() {
        // JPA, and the persistence assembler
    }

    public EmailRecipient getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(EmailRecipient recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public EmailSubject getSubject() {
        return subject;
    }

    public void setSubject(EmailSubject subject) {
        this.subject = subject;
    }

    public EmailContent getContent() {
        return content;
    }

    public void setContent(EmailContent content) {
        this.content = content;
    }

    public boolean isSent() {
        return sent;
    }

    public void setSent(boolean sent) {
        this.sent = sent;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
