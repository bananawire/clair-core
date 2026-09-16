package com.claircore.iam.interfaces.events;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserRegisteredIntegrationEventTest {

    @Test
    void shouldCarryTheUserIdThatWasRegistered() {
        UUID userId = UUID.randomUUID();

        assertEquals(userId, new UserRegisteredIntegrationEvent(userId).userId());
    }

    @Test
    void shouldRejectAnEventWithoutAUser() {
        assertThrows(IllegalArgumentException.class, () -> new UserRegisteredIntegrationEvent(null));
    }
}
