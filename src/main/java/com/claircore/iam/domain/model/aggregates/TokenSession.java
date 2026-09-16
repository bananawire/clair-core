package com.claircore.iam.domain.model.aggregates;

import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.TokenJti;
import com.claircore.iam.domain.model.valueobjects.TokenType;

import java.time.Instant;
import java.util.UUID;

public record TokenSession(
    TokenJti jti,
    EmailAddress email,
    UUID userId,
    TokenType type,
    Instant issuedAt,
    Instant expiresAt
) {
    public TokenSession {
        if (jti == null) throw new IllegalArgumentException("JTI is required");
        if (email == null) throw new IllegalArgumentException("Email is required");
        if (userId == null) throw new IllegalArgumentException("User ID is required");
        if (type == null) throw new IllegalArgumentException("Token type is required");
        if (issuedAt == null) throw new IllegalArgumentException("IssuedAt is required");
        if (expiresAt == null) throw new IllegalArgumentException("ExpiresAt is required");
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}