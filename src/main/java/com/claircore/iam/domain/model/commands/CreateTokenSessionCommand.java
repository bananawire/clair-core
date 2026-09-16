package com.claircore.iam.domain.model.commands;

import com.claircore.iam.domain.model.aggregates.User;
import com.claircore.iam.domain.model.valueobjects.TokenType;

public record CreateTokenSessionCommand(
    User user,
    TokenType type,
    long ttlMillis
) {
    public CreateTokenSessionCommand {
        if (user == null) throw new IllegalArgumentException("User is required");
        if (type == null) throw new IllegalArgumentException("Token type is required");
        if (ttlMillis <= 0) throw new IllegalArgumentException("TTL must be positive");
    }
}
