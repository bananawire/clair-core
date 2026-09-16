package com.claircore.notifications.domain.model.aggregates;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PushNotificationLogTest {

    @Test
    void shouldCreateSentPushNotificationLog() {
        UUID userId = UUID.randomUUID();
        UUID alertId = UUID.randomUUID();

        var log = PushNotificationLog.sent(userId, alertId, "Title", "Message");

        assertTrue(log.isSent());
        assertEquals(userId, log.getUserId());
        assertEquals(alertId, log.getAlertId());
    }

    @Test
    void shouldCreateFailedPushNotificationLog() {
        UUID userId = UUID.randomUUID();

        var log = PushNotificationLog.failed(userId, null, "Title", "Message", "boom");

        assertFalse(log.isSent());
        assertEquals("boom", log.getErrorMessage());
    }
}
