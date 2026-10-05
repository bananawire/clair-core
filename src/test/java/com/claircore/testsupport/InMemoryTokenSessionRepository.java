package com.claircore.testsupport;

import com.claircore.iam.domain.model.aggregates.TokenSession;
import com.claircore.iam.domain.model.valueobjects.TokenJti;
import com.claircore.iam.domain.model.valueobjects.TokenType;
import com.claircore.iam.domain.repositories.TokenSessionRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-process stand-in for the Redis token store so HTTP suites can sign in and revoke tokens
 * without a real Redis.
 */
public final class InMemoryTokenSessionRepository implements TokenSessionRepository {

    private final ConcurrentHashMap<String, TokenSession> byKey = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> userIndex = new ConcurrentHashMap<>();

    @Override
    public void save(TokenSession session) {
        if (session.isExpired()) {
            throw new IllegalArgumentException("Token session TTL must be positive");
        }
        byKey.put(tokenKey(session.jti(), session.type()), session);
        userIndex.put(userKey(session.userId(), session.type()), session.jti().jti());
    }

    @Override
    public void replaceForUser(TokenSession session) {
        String existingJti = userIndex.get(userKey(session.userId(), session.type()));
        if (existingJti != null) {
            byKey.remove(tokenKey(new TokenJti(existingJti), session.type()));
        }
        save(session);
    }

    @Override
    public void revokeAllTokensForUser(UUID userId) {
        deleteIndexed(userId, TokenType.ACCESS);
        deleteIndexed(userId, TokenType.REFRESH);
    }

    @Override
    public Optional<TokenSession> findByJti(TokenJti jti, TokenType type) {
        TokenSession session = byKey.get(tokenKey(jti, type));
        if (session == null || session.isExpired()) {
            if (session != null) {
                deleteByJti(jti, type);
            }
            return Optional.empty();
        }
        return Optional.of(session);
    }

    @Override
    public void deleteByJti(TokenJti jti, TokenType type) {
        TokenSession removed = byKey.remove(tokenKey(jti, type));
        if (removed != null) {
            userIndex.remove(userKey(removed.userId(), type), jti.jti());
        }
    }

    @Override
    public boolean existsByJti(TokenJti jti, TokenType type) {
        return findByJti(jti, type).isPresent();
    }

    public void clear() {
        byKey.clear();
        userIndex.clear();
    }

    private void deleteIndexed(UUID userId, TokenType type) {
        String jti = userIndex.remove(userKey(userId, type));
        if (jti != null) {
            byKey.remove(tokenKey(new TokenJti(jti), type));
        }
    }

    private static String tokenKey(TokenJti jti, TokenType type) {
        return type.name() + ":" + jti.jti();
    }

    private static String userKey(UUID userId, TokenType type) {
        return userId + ":" + type.name();
    }
}
