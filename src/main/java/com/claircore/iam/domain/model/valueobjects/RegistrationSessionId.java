package com.claircore.iam.domain.model.valueobjects;

import java.util.UUID;

public record RegistrationSessionId(String id) {
    public RegistrationSessionId {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Registration session ID cannot be null or empty");
        }
        try {
            UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Registration session ID must be a valid UUID");
        }
    }

    public static RegistrationSessionId generate() {
        return new RegistrationSessionId(UUID.randomUUID().toString());
    }
}
