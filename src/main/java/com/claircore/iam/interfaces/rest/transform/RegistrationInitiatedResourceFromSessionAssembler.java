package com.claircore.iam.interfaces.rest.transform;

import com.claircore.iam.domain.model.aggregates.RegistrationSession;
import com.claircore.iam.interfaces.rest.resources.RegistrationInitiatedResource;

public class RegistrationInitiatedResourceFromSessionAssembler {
    public static RegistrationInitiatedResource toResourceFromSession(RegistrationSession session) {
        return new RegistrationInitiatedResource(
                session.sessionId().id(),
                "Registration initiated. Please check your email for the verification code."
        );
    }
}
