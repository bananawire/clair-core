package com.claircore.iam.domain.model.valueobjects;

import java.util.UUID;

public record UserId(UUID userId) {
    public UserId {
        if (userId == null) throw new IllegalArgumentException("User ID is required");
    }
}
