package com.claircore.iam.infrastructure.persistence.redis.adapters;

import com.claircore.iam.domain.model.aggregates.TokenSession;
import com.claircore.iam.domain.model.valueobjects.TokenJti;
import com.claircore.iam.domain.model.valueobjects.TokenType;
import com.claircore.iam.domain.repositories.TokenSessionRepository;
import com.claircore.iam.infrastructure.persistence.redis.assemblers.TokenSessionRedisAssembler;
import com.claircore.iam.infrastructure.persistence.redis.documents.TokenSessionRedisDocument;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis-backed {@link TokenSessionRepository}. Package-private: the port is the only way in.
 *
 * <p>Both key layouts are unchanged — the session key and the per-user index the
 * one-token-per-type rule depends on — so a token issued before the split still validates after it.
 */
@Repository
class TokenSessionRepositoryImpl implements TokenSessionRepository {

    private static final String KEY_PREFIX = "token:";
    private static final String USER_INDEX_PREFIX = "user:tokens:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    TokenSessionRepositoryImpl(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Retry(name = "redisRepository")
    @CircuitBreaker(name = "redisRepository")
    public void save(TokenSession session) {
        try {
            String value = objectMapper.writeValueAsString(
                    TokenSessionRedisAssembler.toDocumentFromDomain(session));
            long ttlSeconds = Duration.between(Instant.now(), session.expiresAt()).getSeconds();
            if (ttlSeconds <= 0) {
                throw new IllegalArgumentException("Token session TTL must be positive");
            }
            Duration ttl = Duration.ofSeconds(ttlSeconds);

            redisTemplate.opsForValue().set(buildTokenKey(session.jti().jti(), session.type()), value, ttl);
            redisTemplate.opsForValue().set(
                    buildUserIndexKey(session.userId(), session.type()), session.jti().jti(), ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize token session", e);
        }
    }

    @Override
    @Retry(name = "redisRepository")
    @CircuitBreaker(name = "redisRepository")
    public void replaceForUser(TokenSession session) {
        UUID userId = session.userId();
        TokenType type = session.type();

        String existingJti = redisTemplate.opsForValue().get(buildUserIndexKey(userId, type));
        if (existingJti != null) {
            redisTemplate.delete(buildTokenKey(existingJti, type));
        }

        save(session);
    }

    @Override
    @Retry(name = "redisRepository")
    @CircuitBreaker(name = "redisRepository")
    public void revokeAllTokensForUser(UUID userId) {
        String accessIndex = buildUserIndexKey(userId, TokenType.ACCESS);
        String refreshIndex = buildUserIndexKey(userId, TokenType.REFRESH);

        String accessJti = redisTemplate.opsForValue().get(accessIndex);
        String refreshJti = redisTemplate.opsForValue().get(refreshIndex);

        if (accessJti != null) {
            redisTemplate.delete(buildTokenKey(accessJti, TokenType.ACCESS));
        }
        if (refreshJti != null) {
            redisTemplate.delete(buildTokenKey(refreshJti, TokenType.REFRESH));
        }

        redisTemplate.delete(accessIndex);
        redisTemplate.delete(refreshIndex);
    }

    @Override
    @Retry(name = "redisRepository")
    @CircuitBreaker(name = "redisRepository")
    public Optional<TokenSession> findByJti(TokenJti jti, TokenType type) {
        String value = redisTemplate.opsForValue().get(buildTokenKey(jti.jti(), type));
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(TokenSessionRedisAssembler.toDomainFromDocument(
                    objectMapper.readValue(value, TokenSessionRedisDocument.class)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize token session", e);
        }
    }

    @Override
    @Retry(name = "redisRepository")
    @CircuitBreaker(name = "redisRepository")
    public void deleteByJti(TokenJti jti, TokenType type) {
        redisTemplate.delete(buildTokenKey(jti.jti(), type));
    }

    @Override
    @Retry(name = "redisRepository")
    @CircuitBreaker(name = "redisRepository")
    public boolean existsByJti(TokenJti jti, TokenType type) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(buildTokenKey(jti.jti(), type)));
    }

    private String buildTokenKey(String jti, TokenType type) {
        return KEY_PREFIX + type.name().toLowerCase() + ":" + jti;
    }

    private String buildUserIndexKey(UUID userId, TokenType type) {
        return USER_INDEX_PREFIX + userId + ":" + type.name().toLowerCase();
    }
}
