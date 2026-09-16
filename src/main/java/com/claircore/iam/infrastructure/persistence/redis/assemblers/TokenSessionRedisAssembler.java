package com.claircore.iam.infrastructure.persistence.redis.assemblers;

import com.claircore.iam.domain.model.aggregates.TokenSession;
import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.TokenJti;
import com.claircore.iam.domain.model.valueobjects.TokenType;
import com.claircore.iam.infrastructure.persistence.redis.documents.TokenSessionRedisDocument;

public final class TokenSessionRedisAssembler {

    private TokenSessionRedisAssembler() {
    }

    public static TokenSession toDomainFromDocument(TokenSessionRedisDocument document) {
        if (document == null) return null;
        return new TokenSession(
                new TokenJti(document.jti().jti()),
                new EmailAddress(document.email().address()),
                document.userId(),
                TokenType.valueOf(document.type()),
                document.issuedAt(),
                document.expiresAt());
    }

    public static TokenSessionRedisDocument toDocumentFromDomain(TokenSession session) {
        if (session == null) return null;
        return new TokenSessionRedisDocument(
                new TokenSessionRedisDocument.JtiField(session.jti().jti()),
                new TokenSessionRedisDocument.EmailField(session.email().address()),
                session.userId(),
                session.type().name(),
                session.issuedAt(),
                session.expiresAt());
    }
}
