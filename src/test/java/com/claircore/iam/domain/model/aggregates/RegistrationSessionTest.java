package com.claircore.iam.domain.model.aggregates;

import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.RegistrationSessionId;
import com.claircore.iam.domain.model.valueobjects.VerificationCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationSessionTest {

    @Test
    void shouldCreateNonExpiredSessionWhenTtlIsPositive() {
        RegistrationSession session = new RegistrationSession(
                new RegistrationSessionId(UUID.randomUUID().toString()),
                new EmailAddress("user@example.com"),
                "encoded-password",
                new VerificationCode("6G13-789D"),
                30
        );

        assertFalse(session.isExpired());
    }

    @Test
    void shouldReportExpiredWhenExpirationInstantIsInThePast() {
        RegistrationSession session = new RegistrationSession(
                new RegistrationSessionId(UUID.randomUUID().toString()),
                new EmailAddress("user@example.com"),
                "encoded-password",
                new VerificationCode("6G13-789D"),
                Instant.now().minusSeconds(10),
                Instant.now().minusSeconds(1)
        );

        assertTrue(session.isExpired());
    }

    @Test
    void shouldVerifyCodeOnlyWhenCodeMatchesAndSessionIsActive() {
        RegistrationSession session = new RegistrationSession(
                new RegistrationSessionId(UUID.randomUUID().toString()),
                new EmailAddress("user@example.com"),
                "encoded-password",
                new VerificationCode("6G13-789D"),
                30
        );

        assertTrue(session.verifyCode("6G13-789D"));
    }
}
