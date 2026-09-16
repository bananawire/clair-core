package com.claircore.iam.interfaces.rest.transform;

import com.claircore.iam.domain.model.aggregates.RegistrationSession;
import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.RegistrationSessionId;
import com.claircore.iam.domain.model.valueobjects.VerificationCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegistrationInitiatedResourceFromSessionAssemblerTest {

    @Test
    void shouldMapSessionToResourceWithFriendlyMessage() {
        RegistrationSession session = new RegistrationSession(
                RegistrationSessionId.generate(),
                new EmailAddress("user@example.com"),
                "encoded-password",
                new VerificationCode("6G13-789D"),
                30
        );

        var resource = RegistrationInitiatedResourceFromSessionAssembler.toResourceFromSession(session);

        assertEquals(session.sessionId().id(), resource.sessionId());
        assertEquals("Registration initiated. Please check your email for the verification code.", resource.message());
    }
}
