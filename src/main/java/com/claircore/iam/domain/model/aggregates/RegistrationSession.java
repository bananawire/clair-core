package com.claircore.iam.domain.model.aggregates;

import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.RegistrationSessionId;
import com.claircore.iam.domain.model.valueobjects.VerificationCode;

import java.time.Instant;

public record RegistrationSession(
    RegistrationSessionId sessionId,
    EmailAddress email,
    String passwordHash,
    VerificationCode verificationCode,
    Instant createdAt,
    Instant expiresAt
) {
    public RegistrationSession(RegistrationSessionId sessionId, EmailAddress email, String passwordHash, VerificationCode verificationCode, long ttlMinutes) {
        this(sessionId, email, passwordHash, verificationCode, Instant.now(), Instant.now().plusSeconds(ttlMinutes * 60));
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean verifyCode(String code) {
        return !isExpired() && verificationCode.matches(code);
    }
}
