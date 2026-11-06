package com.claircore.iam.application.internal.commandservices;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/** Explicit, loopback-only development opt-in; ordinary registration always verifies email. */
@Component
public class RegistrationPolicy {
    private final boolean localAutoConfirm;

    public RegistrationPolicy(
            @Value("${iam.registration.local-auto-confirm:false}") boolean localAutoConfirm,
            Environment environment) {
        if (localAutoConfirm && (!environment.acceptsProfiles(Profiles.of("local & !prod & !production"))
                || !"127.0.0.1".equals(environment.getProperty("server.address")))) {
            throw new IllegalStateException(
                    "Local auto-confirm requires the local profile and server.address=127.0.0.1; never enable it in production");
        }
        this.localAutoConfirm = localAutoConfirm;
    }

    public boolean emailVerificationRequired() {
        return !localAutoConfirm;
    }
}
