package com.claircore.assistant.infrastructure.persistence.jpa.entities;

import com.claircore.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Storage shape of {@code Conversation}. */
@Entity
@Table(name = "assistant_conversations")
public class ConversationPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(name = "last_message_at", nullable = false)
    private Instant lastMessageAt;

    public ConversationPersistenceEntity() {
        // JPA, and the persistence assembler
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Instant getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(Instant lastMessageAt) { this.lastMessageAt = lastMessageAt; }
}
