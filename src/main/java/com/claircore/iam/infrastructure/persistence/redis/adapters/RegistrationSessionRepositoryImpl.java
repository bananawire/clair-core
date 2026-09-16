package com.claircore.iam.infrastructure.persistence.redis.adapters;

import com.claircore.iam.domain.model.aggregates.RegistrationSession;
import com.claircore.iam.domain.model.valueobjects.RegistrationSessionId;
import com.claircore.iam.domain.repositories.RegistrationSessionRepository;
import com.claircore.iam.infrastructure.persistence.redis.assemblers.RegistrationSessionRedisAssembler;
import com.claircore.iam.infrastructure.persistence.redis.documents.RegistrationSessionRedisDocument;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-backed {@link RegistrationSessionRepository}. Package-private: the port is the only way in.
 *
 * <p>The key layout and the TTL are unchanged — a session written before the split is still found
 * by a instance running after it.
 */
@Repository
class RegistrationSessionRepositoryImpl implements RegistrationSessionRepository {

    private static final String KEY_PREFIX = "registration:session:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    RegistrationSessionRepositoryImpl(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Retry(name = "redisRepository")
    @CircuitBreaker(name = "redisRepository")
    public void save(RegistrationSession session) {
        try {
            String key = buildKey(session.sessionId().id());
            String value = objectMapper.writeValueAsString(
                    RegistrationSessionRedisAssembler.toDocumentFromDomain(session));
            long ttlSeconds = Duration.between(session.createdAt(), session.expiresAt()).getSeconds();
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize registration session", e);
        }
    }

    @Override
    @Retry(name = "redisRepository")
    @CircuitBreaker(name = "redisRepository")
    public Optional<RegistrationSession> findById(RegistrationSessionId sessionId) {
        String value = redisTemplate.opsForValue().get(buildKey(sessionId.id()));
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(RegistrationSessionRedisAssembler.toDomainFromDocument(
                    objectMapper.readValue(value, RegistrationSessionRedisDocument.class)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize registration session", e);
        }
    }

    @Override
    @Retry(name = "redisRepository")
    @CircuitBreaker(name = "redisRepository")
    public void deleteById(RegistrationSessionId sessionId) {
        redisTemplate.delete(buildKey(sessionId.id()));
    }

    private String buildKey(String sessionId) {
        return KEY_PREFIX + sessionId;
    }
}
