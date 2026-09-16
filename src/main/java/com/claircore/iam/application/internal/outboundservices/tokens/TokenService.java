package com.claircore.iam.application.internal.outboundservices.tokens;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for minting and reading the signed tokens handed to clients. The application
 * decides what a token means; how it is signed and parsed is an infrastructure choice.
 *
 * <p>Every {@code extract} returns empty rather than throwing on a token that is malformed,
 * expired, or signed with another key: an unreadable token is a failed authentication, not a fault.
 */
public interface TokenService {

    String generateAccessToken(UUID userId, long ttlMillis, String jti);

    String generateRefreshToken(UUID userId, long ttlMillis, String jti);

    Optional<String> extractJti(String token);

    Optional<UUID> extractUserId(String token);

    /** The token's type claim, lowercase: {@code access} or {@code refresh}. */
    Optional<String> extractType(String token);
}
