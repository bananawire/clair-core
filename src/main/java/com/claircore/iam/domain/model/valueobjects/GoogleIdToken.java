package com.claircore.iam.domain.model.valueobjects;

public record GoogleIdToken(String token) {
    public GoogleIdToken {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Google ID token cannot be null or empty");
        }
    }
}
