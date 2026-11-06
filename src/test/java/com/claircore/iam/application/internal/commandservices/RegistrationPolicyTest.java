package com.claircore.iam.application.internal.commandservices;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class RegistrationPolicyTest {
    @Test
    void requiresEmailVerificationByDefault() {
        assertTrue(new RegistrationPolicy(false, new MockEnvironment()).emailVerificationRequired());
    }

    @Test
    void allowsExplicitLocalOptInOnlyOnLoopback() {
        var environment = new MockEnvironment().withProperty("server.address", "127.0.0.1");
        environment.setActiveProfiles("local");
        assertFalse(new RegistrationPolicy(true, environment).emailVerificationRequired());
    }

    @Test
    void rejectsOptInWithoutLocalProfile() {
        var environment = new MockEnvironment().withProperty("server.address", "127.0.0.1");
        assertThrows(IllegalStateException.class, () -> new RegistrationPolicy(true, environment));
    }

    @Test
    void rejectsPublicBindAddress() {
        var environment = new MockEnvironment().withProperty("server.address", "0.0.0.0");
        environment.setActiveProfiles("local");
        assertThrows(IllegalStateException.class, () -> new RegistrationPolicy(true, environment));
    }

    @Test
    void rejectsProductionEvenWhenLocalProfileIsAlsoPresent() {
        for (var profile : new String[]{"prod", "production"}) {
            var environment = new MockEnvironment().withProperty("server.address", "127.0.0.1");
            environment.setActiveProfiles("local", profile);
            assertThrows(IllegalStateException.class, () -> new RegistrationPolicy(true, environment));
        }
    }
}
