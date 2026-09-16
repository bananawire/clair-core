package com.claircore.iam.infrastructure.persistence.redis.documents;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Storage shape of {@code TokenSession}, as JSON in Redis.
 *
 * <p>Field names are wire names — a live token written by an earlier deploy must still deserialize,
 * so {@code jti} and {@code email} keep the single-field object nesting the aggregate serialized
 * them with. Every access token in the fleet is validated against this shape.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TokenSessionRedisDocument(
        JtiField jti,
        EmailField email,
        UUID userId,
        String type,
        Instant issuedAt,
        Instant expiresAt
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JtiField(String jti) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EmailField(String address) {}
}
