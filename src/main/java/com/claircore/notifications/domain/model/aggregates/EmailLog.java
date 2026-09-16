package com.claircore.notifications.domain.model.aggregates;

import com.claircore.notifications.domain.model.valueobjects.EmailContent;
import com.claircore.notifications.domain.model.valueobjects.EmailRecipient;
import com.claircore.notifications.domain.model.valueobjects.EmailSubject;

import java.time.Instant;
import java.util.UUID;

/**
 * Record of one outbound email, successful or failed. Append-only: nothing mutates an email log
 * after it is written, so the aggregate has no behaviour beyond its two factories.
 */
public class EmailLog {

    private final UUID id;
    private final EmailRecipient recipientEmail;
    private final EmailSubject subject;
    private final EmailContent content;
    private final boolean sent;
    private final String errorMessage;
    private final Instant createdAt;
    private final Instant updatedAt;

    private EmailLog(
            UUID id,
            EmailRecipient recipientEmail,
            EmailSubject subject,
            EmailContent content,
            boolean sent,
            String errorMessage,
            Instant createdAt,
            Instant updatedAt) {
        if (id == null) throw new IllegalArgumentException("Id is required");
        if (recipientEmail == null) throw new IllegalArgumentException("Recipient email is required");
        if (subject == null) throw new IllegalArgumentException("Email subject is required");
        if (content == null) throw new IllegalArgumentException("Email content is required");
        this.id = id;
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.content = content;
        this.sent = sent;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public EmailLog(EmailRecipient recipientEmail, EmailSubject subject, EmailContent content, boolean sent, String errorMessage) {
        this(UUID.randomUUID(), recipientEmail, subject, content, sent, errorMessage, null, null);
    }

    public static EmailLog sent(EmailRecipient recipientEmail, EmailSubject subject, EmailContent content) {
        return new EmailLog(recipientEmail, subject, content, true, null);
    }

    public static EmailLog failed(EmailRecipient recipientEmail, EmailSubject subject, EmailContent content, String errorMessage) {
        return new EmailLog(recipientEmail, subject, content, false, errorMessage);
    }

    /** Rebuilds a log that already exists in storage, identity and audit timestamps included. */
    public static EmailLog reconstitute(
            UUID id,
            EmailRecipient recipientEmail,
            EmailSubject subject,
            EmailContent content,
            boolean sent,
            String errorMessage,
            Instant createdAt,
            Instant updatedAt) {
        return new EmailLog(id, recipientEmail, subject, content, sent, errorMessage, createdAt, updatedAt);
    }

    public UUID getId() {
        return id;
    }

    public EmailRecipient getRecipientEmail() {
        return recipientEmail;
    }

    public EmailSubject getSubject() {
        return subject;
    }

    public EmailContent getContent() {
        return content;
    }

    public boolean isSent() {
        return sent;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /** Null until the log has been written; assigned by persistence auditing. */
    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
