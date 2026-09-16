package com.claircore.iam.domain.repositories;

import com.claircore.iam.domain.model.aggregates.TokenSession;
import com.claircore.iam.domain.model.valueobjects.TokenJti;
import com.claircore.iam.domain.model.valueobjects.TokenType;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for issued-token storage. A session expires on its own; the operations here are the ones
 * that end a session early, and the one-token-per-user-per-type rule {@code replaceForUser} keeps.
 *
 * <p>Storage for these is remote and may be unreachable. The adapter does not translate that into
 * an absent session: a read that cannot reach storage throws rather than returning
 * {@code Optional.empty()}, because an empty Optional here means "this token was revoked" and
 * answering that on an outage would sign every user out.
 */
public interface TokenSessionRepository {

    void save(TokenSession session);

    /** Stores the session and drops whatever token the user held of the same type. */
    void replaceForUser(TokenSession session);

    void revokeAllTokensForUser(UUID userId);

    Optional<TokenSession> findByJti(TokenJti jti, TokenType type);

    void deleteByJti(TokenJti jti, TokenType type);

    boolean existsByJti(TokenJti jti, TokenType type);
}
