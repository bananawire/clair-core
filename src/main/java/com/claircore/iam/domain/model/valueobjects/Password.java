package com.claircore.iam.domain.model.valueobjects;

/** A password that has already been hashed; the plaintext never reaches the domain. */
public record Password(String passwordHash) {
    public Password {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
    }
}
