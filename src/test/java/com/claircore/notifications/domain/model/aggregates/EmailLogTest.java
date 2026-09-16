package com.claircore.notifications.domain.model.aggregates;

import com.claircore.notifications.domain.model.valueobjects.EmailContent;
import com.claircore.notifications.domain.model.valueobjects.EmailRecipient;
import com.claircore.notifications.domain.model.valueobjects.EmailSubject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailLogTest {

    @Test
    void shouldCreateSentEmailLog() {
        var log = EmailLog.sent(new EmailRecipient("user@example.com"), new EmailSubject("Welcome"), new EmailContent("<p>Hello</p>"));

        assertTrue(log.isSent());
        assertEquals("user@example.com", log.getRecipientEmail().address());
    }

    @Test
    void shouldCreateFailedEmailLog() {
        var log = EmailLog.failed(new EmailRecipient("user@example.com"), new EmailSubject("Welcome"), new EmailContent("<p>Hello</p>"), "boom");

        assertFalse(log.isSent());
        assertEquals("boom", log.getErrorMessage());
    }
}
