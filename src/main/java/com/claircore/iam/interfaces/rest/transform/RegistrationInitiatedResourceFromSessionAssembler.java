package com.claircore.iam.interfaces.rest.transform;

import com.claircore.iam.domain.model.aggregates.RegistrationSession;
import com.claircore.iam.interfaces.rest.resources.RegistrationInitiatedResource;

public class RegistrationInitiatedResourceFromSessionAssembler {
    public static RegistrationInitiatedResource toResourceFromSession(RegistrationSession session) {
        return toResourceFromSession(session, true);
    }

    public static RegistrationInitiatedResource toResourceFromSession(RegistrationSession session, boolean verificationRequired) {
        return new RegistrationInitiatedResource(
                session.sessionId().id(),
                verificationRequired
                        ? "Registration initiated. Please check your email for the verification code."
                        : "Local account created. You can sign in with your email and password.",
                verificationRequired
        );
    }
}
