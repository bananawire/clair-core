package com.claircore.iam.domain.model.aggregates;

import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.TokenJti;
import com.claircore.iam.domain.model.valueobjects.TokenType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenSessionTest {

    @Test
    void shouldCreateNonExpiredTokenSessionWhenExpirationIsInTheFuture() {
        TokenSession session = new TokenSession(
                new TokenJti(UUID.randomUUID().toString()),
                new EmailAddress("user@example.com"),
                UUID.randomUUID(),
                TokenType.ACCESS,
                Instant.now(),
                Instant.now().plusSeconds(60)
        );

        assertFalse(session.isExpired());
    }

    @Test
    void shouldReportExpiredWhenExpirationIsInThePast() {
        TokenSession session = new TokenSession(
                new TokenJti(UUID.randomUUID().toString()),
                new EmailAddress("user@example.com"),
                UUID.randomUUID(),
                TokenType.REFRESH,
                Instant.now().minusSeconds(120),
                Instant.now().minusSeconds(60)
        );

        assertTrue(session.isExpired());
    }
}
