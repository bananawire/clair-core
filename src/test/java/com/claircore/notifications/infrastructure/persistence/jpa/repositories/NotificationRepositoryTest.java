package com.claircore.notifications.infrastructure.persistence.jpa.repositories;

import com.claircore.notifications.domain.model.aggregates.EmailLog;
import com.claircore.notifications.domain.model.aggregates.PushNotificationLog;
import com.claircore.notifications.domain.model.valueobjects.EmailContent;
import com.claircore.notifications.domain.model.valueobjects.EmailRecipient;
import com.claircore.notifications.domain.model.valueobjects.EmailSubject;
import com.claircore.notifications.domain.repositories.EmailLogRepository;
import com.claircore.notifications.domain.repositories.PushNotificationLogRepository;
import com.claircore.notifications.infrastructure.persistence.jpa.adapters.EmailLogRepositoryImpl;
import com.claircore.notifications.infrastructure.persistence.jpa.adapters.PushNotificationLogRepositoryImpl;
import com.claircore.shared.infrastructure.config.JpaAuditingConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exercises the ports through their adapters, which is the only path production code uses. */
@DataJpaTest
@Import({JpaAuditingConfiguration.class, EmailLogRepositoryImpl.class, PushNotificationLogRepositoryImpl.class})
class NotificationRepositoryTest {

    @Autowired
    private EmailLogRepository emailLogRepository;

    @Autowired
    private PushNotificationLogRepository pushNotificationLogRepository;

    @Test
    void shouldFindEmailLogsByRecipient() {
        var recipient = new EmailRecipient("user@example.com");
        emailLogRepository.save(EmailLog.sent(recipient, new EmailSubject("Welcome"), new EmailContent("<p>Hello</p>")));

        var logs = emailLogRepository.findByRecipient(recipient);

        assertEquals(1, logs.size());
        assertEquals(recipient, logs.getFirst().getRecipientEmail());
        assertEquals("<p>Hello</p>", logs.getFirst().getContent().html());
    }

    @Test
    void shouldKeepTheIdentityTheAggregateAssignedAndFillTheAuditTimestamps() {
        var log = EmailLog.sent(new EmailRecipient("user@example.com"), new EmailSubject("Welcome"), new EmailContent("<p>Hello</p>"));

        var saved = emailLogRepository.save(log);

        assertEquals(log.getId(), saved.getId(), "the aggregate assigns the id, not the database");
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void shouldPagePushNotificationLogsByUserId() {
        UUID userId = UUID.randomUUID();
        pushNotificationLogRepository.save(PushNotificationLog.sent(userId, UUID.randomUUID(), "Title", "Message"));
        pushNotificationLogRepository.save(PushNotificationLog.sent(UUID.randomUUID(), UUID.randomUUID(), "Other", "Other"));

        var page = pushNotificationLogRepository.findByUserId(userId, 0, 10);

        assertEquals(1, page.total());
        assertEquals(0, page.page());
        assertEquals(10, page.size());
        assertTrue(page.items().getFirst().isSent());
        assertEquals("Title", page.items().getFirst().getTitle());
    }
}
