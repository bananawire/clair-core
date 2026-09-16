package com.claircore.iam.infrastructure.persistence.redis.assemblers;

import com.claircore.iam.domain.model.aggregates.RegistrationSession;
import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.RegistrationSessionId;
import com.claircore.iam.domain.model.valueobjects.VerificationCode;
import com.claircore.iam.infrastructure.persistence.redis.documents.RegistrationSessionRedisDocument;

public final class RegistrationSessionRedisAssembler {

    private RegistrationSessionRedisAssembler() {
    }

    public static RegistrationSession toDomainFromDocument(RegistrationSessionRedisDocument document) {
        if (document == null) return null;
        return new RegistrationSession(
                new RegistrationSessionId(document.sessionId().id()),
                new EmailAddress(document.email().address()),
                document.passwordHash(),
                new VerificationCode(document.verificationCode().code()),
                document.createdAt(),
                document.expiresAt());
    }

    public static RegistrationSessionRedisDocument toDocumentFromDomain(RegistrationSession session) {
        if (session == null) return null;
        return new RegistrationSessionRedisDocument(
                new RegistrationSessionRedisDocument.SessionIdField(session.sessionId().id()),
                new RegistrationSessionRedisDocument.EmailField(session.email().address()),
                session.passwordHash(),
                new RegistrationSessionRedisDocument.VerificationCodeField(session.verificationCode().code()),
                session.createdAt(),
                session.expiresAt());
    }
}
