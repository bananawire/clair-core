package com.claircore.notifications.domain.model.queries;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class GetPushNotificationHistoryQueryTest {

    @Test
    void shouldCreateQueryWhenArgumentsAreValid() {
        UUID userId = UUID.randomUUID();

        var query = new GetPushNotificationHistoryQuery(userId, 0, 20);

        assertEquals(userId, query.userId());
        assertEquals(0, query.page());
        assertEquals(20, query.size());
    }

    @Test
    void shouldRejectNullUserId() {
        var exception = assertThrowsExactly(IllegalArgumentException.class, () -> new GetPushNotificationHistoryQuery(null, 0, 20));

        assertEquals("User ID is required", exception.getMessage());
    }

    @Test
    void shouldRejectNegativePage() {
        var exception = assertThrowsExactly(IllegalArgumentException.class, () -> new GetPushNotificationHistoryQuery(UUID.randomUUID(), -1, 20));

        assertEquals("Page must not be negative", exception.getMessage());
    }

    @Test
    void shouldRejectNonPositiveSize() {
        var exception = assertThrowsExactly(IllegalArgumentException.class, () -> new GetPushNotificationHistoryQuery(UUID.randomUUID(), 0, 0));

        assertEquals("Size must be positive", exception.getMessage());
    }
}
