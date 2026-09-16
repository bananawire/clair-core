package com.claircore.iam.domain.model.valueobjects;

import java.util.UUID;

public record TokenJti(String jti) {
    public TokenJti {
        if (jti == null || jti.isBlank()) {
            throw new IllegalArgumentException("Token JTI cannot be null or empty");
        }
        try {
            UUID.fromString(jti);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Token JTI must be a valid UUID");
        }
    }

    public static TokenJti generate() {
        return new TokenJti(UUID.randomUUID().toString());
    }
}
