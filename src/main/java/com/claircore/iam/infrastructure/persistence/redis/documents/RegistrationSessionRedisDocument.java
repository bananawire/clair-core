package com.claircore.iam.infrastructure.persistence.redis.documents;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * Storage shape of {@code RegistrationSession}, as JSON in Redis.
 *
 * <p>Every field name here is a wire name: sessions written by an earlier deploy are still in
 * Redis and must still deserialize, so the nesting is the aggregate's own — {@code sessionId} and
 * {@code verificationCode} were serialized as objects wrapping a single field, not as bare strings,
 * and they stay that way. {@code ignoreUnknown} keeps a session written by a newer deploy readable.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RegistrationSessionRedisDocument(
        SessionIdField sessionId,
        EmailField email,
        String passwordHash,
        VerificationCodeField verificationCode,
        Instant createdAt,
        Instant expiresAt
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SessionIdField(String id) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EmailField(String address) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VerificationCodeField(String code) {}
}
