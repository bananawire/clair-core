package com.claircore.iam.domain.model.valueobjects;

public record VerificationCode(String code) {
    public VerificationCode {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Verification code cannot be null or empty");
        }
        if (!code.matches("^[A-Z0-9]{4}-[A-Z0-9]{4}$")) {
            throw new IllegalArgumentException("Verification code must be in format XXXX-XXXX (uppercase alphanumeric)");
        }
    }

    public boolean matches(String other) {
        return code.equals(other);
    }
}
