package com.claircore.iam.domain.model.aggregates;

import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.OAuthProvider;
import com.claircore.iam.domain.model.valueobjects.Password;
import com.claircore.iam.domain.model.valueobjects.UserStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    void shouldCreatePendingVerificationUserWhenMailCredentialsAreProvided() {
        User user = new User(new EmailAddress("user@example.com"), new Password("encoded-password"));

        assertEquals(UserStatus.PENDING_VERIFICATION, user.getStatus());
        assertFalse(user.isActive());
        assertFalse(user.isOAuthUser());
    }

    @Test
    void shouldActivateUserWhenActivationIsRequested() {
        User user = new User(new EmailAddress("user@example.com"), new Password("encoded-password"));

        user.activate();

        assertTrue(user.isActive());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }

    @Test
    void shouldLinkOAuthAccountWhenOAuthProviderIsProvided() {
        User user = new User(new EmailAddress("user@example.com"), new Password("encoded-password"));

        user.linkOAuthAccount(OAuthProvider.GOOGLE, "google-subject");

        assertTrue(user.isOAuthUser());
        assertEquals(OAuthProvider.GOOGLE, user.getOauthProvider());
        assertEquals("google-subject", user.getOauthSubject());
    }

    @Test
    void shouldCreateActiveOAuthUserWhenGoogleConstructorIsUsed() {
        User user = new User(new EmailAddress("user@example.com"), OAuthProvider.GOOGLE, "google-subject");

        assertTrue(user.isActive());
        assertTrue(user.isOAuthUser());
        assertEquals(OAuthProvider.GOOGLE, user.getOauthProvider());
    }
}
