package com.claircore.notifications.infrastructure.persistence.jpa.assemblers;

import com.claircore.notifications.domain.model.aggregates.EmailLog;
import com.claircore.notifications.domain.model.aggregates.PushNotificationLog;
import com.claircore.notifications.domain.model.valueobjects.EmailContent;
import com.claircore.notifications.domain.model.valueobjects.EmailRecipient;
import com.claircore.notifications.domain.model.valueobjects.EmailSubject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationPersistenceAssemblerTest {

    @Test
    void emailLogSurvivesARoundTrip() {
        var original = EmailLog.reconstitute(
                UUID.randomUUID(),
                new EmailRecipient("user@example.com"),
                new EmailSubject("Welcome"),
                new EmailContent("<p>Hello</p>"),
                false,
                "smtp down",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z"));

        var roundTripped = EmailLogPersistenceAssembler.toDomainFromPersistence(
                EmailLogPersistenceAssembler.toPersistenceFromDomain(original));

        assertEquals(original.getId(), roundTripped.getId());
        assertEquals(original.getRecipientEmail(), roundTripped.getRecipientEmail());
        assertEquals(original.getSubject(), roundTripped.getSubject());
        assertEquals(original.getContent(), roundTripped.getContent());
        assertFalse(roundTripped.isSent());
        assertEquals("smtp down", roundTripped.getErrorMessage());
        assertEquals(original.getCreatedAt(), roundTripped.getCreatedAt());
        assertEquals(original.getUpdatedAt(), roundTripped.getUpdatedAt());
    }

    @Test
    void pushNotificationLogSurvivesARoundTrip() {
        var original = PushNotificationLog.reconstitute(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Title",
                "Message",
                true,
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z"));

        var roundTripped = PushNotificationLogPersistenceAssembler.toDomainFromPersistence(
                PushNotificationLogPersistenceAssembler.toPersistenceFromDomain(original));

        assertEquals(original.getId(), roundTripped.getId());
        assertEquals(original.getUserId(), roundTripped.getUserId());
        assertEquals(original.getAlertId(), roundTripped.getAlertId());
        assertEquals("Title", roundTripped.getTitle());
        assertEquals("Message", roundTripped.getMessage());
        assertTrue(roundTripped.isSent());
        assertNull(roundTripped.getErrorMessage());
        assertEquals(original.getCreatedAt(), roundTripped.getCreatedAt());
        assertEquals(original.getUpdatedAt(), roundTripped.getUpdatedAt());
    }

    @Test
    void anUnsavedLogMapsToAnEntityThatCountsAsNew() {
        var entity = EmailLogPersistenceAssembler.toPersistenceFromDomain(
                EmailLog.sent(new EmailRecipient("user@example.com"), new EmailSubject("Welcome"), new EmailContent("<p>Hello</p>")));

        assertTrue(entity.isNew(), "a log with no createdAt must persist, not merge");
    }

    @Test
    void aStoredLogMapsToAnEntityThatIsNotNew() {
        var entity = PushNotificationLogPersistenceAssembler.toPersistenceFromDomain(
                PushNotificationLog.reconstitute(
                        UUID.randomUUID(), UUID.randomUUID(), null, "Title", "Message", true, null,
                        Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z")));

        assertFalse(entity.isNew());
    }

    @Test
    void assemblersAreNullSafe() {
        assertNull(EmailLogPersistenceAssembler.toDomainFromPersistence(null));
        assertNull(EmailLogPersistenceAssembler.toPersistenceFromDomain(null));
        assertNull(PushNotificationLogPersistenceAssembler.toDomainFromPersistence(null));
        assertNull(PushNotificationLogPersistenceAssembler.toPersistenceFromDomain(null));
    }
}
