package com.claircore.iam.domain.model.aggregates;

import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.OAuthProvider;
import com.claircore.iam.domain.model.valueobjects.Password;
import com.claircore.iam.domain.model.valueobjects.UserStatus;

import java.time.Instant;
import java.util.UUID;

/** One account, however it authenticates: local credentials or a linked OAuth identity. */
public class User {

    private final UUID id;
    private final EmailAddress email;
    private final Password password;
    private UserStatus status;
    private OAuthProvider oauthProvider;
    private String oauthSubject;
    private final Instant createdAt;
    private final Instant updatedAt;

    private User(UUID id, EmailAddress email, Password password, UserStatus status,
                 OAuthProvider oauthProvider, String oauthSubject, Instant createdAt, Instant updatedAt) {
        if (id == null) {
            throw new IllegalArgumentException("Id must not be null");
        }
        if (email == null) {
            throw new IllegalArgumentException("Email must not be null");
        }
        if (password == null) {
            throw new IllegalArgumentException("Password must not be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status must not be null");
        }
        this.id = id;
        this.email = email;
        this.password = password;
        this.status = status;
        this.oauthProvider = oauthProvider;
        this.oauthSubject = oauthSubject;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public User(EmailAddress email, Password password) {
        this(UUID.randomUUID(), email, password, UserStatus.PENDING_VERIFICATION,
                OAuthProvider.MAIL, null, null, null);
    }

    public User(EmailAddress email, OAuthProvider oauthProvider, String oauthSubject) {
        this(UUID.randomUUID(), email, generateRandomPassword(), UserStatus.ACTIVE,
                oauthProvider, oauthSubject, null, null);
    }

    /**
     * Rebuilds a user already in storage. The persistence assembler is the only caller; it is the
     * one path that may set an identity and audit timestamps rather than generating them.
     */
    public static User rehydrate(
            UUID id,
            EmailAddress email,
            Password password,
            UserStatus status,
            OAuthProvider oauthProvider,
            String oauthSubject,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new User(id, email, password, status, oauthProvider, oauthSubject, createdAt, updatedAt);
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void linkOAuthAccount(OAuthProvider provider, String subject) {
        this.oauthProvider = provider;
        this.oauthSubject = subject;
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    public boolean isOAuthUser() {
        return this.oauthProvider != null && this.oauthProvider != OAuthProvider.MAIL;
    }

    public UUID getId() {
        return id;
    }

    public EmailAddress getEmail() {
        return email;
    }

    public Password getPassword() {
        return password;
    }

    public UserStatus getStatus() {
        return status;
    }

    public OAuthProvider getOauthProvider() {
        return oauthProvider;
    }

    public String getOauthSubject() {
        return oauthSubject;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * An OAuth user never signs in with a password, but the column is not nullable and a blank hash
     * would match nothing at all only by accident. A random one makes that explicit.
     */
    private static Password generateRandomPassword() {
        return new Password(UUID.randomUUID() + String.valueOf(System.currentTimeMillis()));
    }
}
