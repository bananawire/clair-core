package com.claircore.testsupport;

import com.claircore.evaluation.domain.repositories.TelemetryEvaluationRepository;
import com.claircore.evaluation.infrastructure.persistence.jpa.adapters.TelemetryEvaluationRepositoryImpl;
import com.claircore.evaluation.infrastructure.persistence.jpa.repositories.TelemetryEvaluationPersistenceRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Shared doubles for HTTP suites (BDD and system tests): H2 holds the domain tables,
 * sessions live in memory, and the cache is a no-op so Redis is never contacted.
 */
@TestConfiguration
public class HermeticHttpTestConfiguration {

    @Bean
    @Primary
    InMemoryTokenSessionRepository inMemoryTokenSessionRepository() {
        return new InMemoryTokenSessionRepository();
    }

    @Bean
    @Primary
    InMemoryRegistrationSessionRepository inMemoryRegistrationSessionRepository() {
        return new InMemoryRegistrationSessionRepository();
    }

    @Bean
    @Primary
    CacheManager hermeticCacheManager() {
        return new NoOpCacheManager();
    }

    @Bean
    @Primary
    TelemetryEvaluationRepository h2CompatibleTelemetryEvaluationRepository(
            TelemetryEvaluationRepositoryImpl delegate,
            TelemetryEvaluationPersistenceRepository jpa) {
        return new H2CompatibleTelemetryEvaluationRepository(delegate, jpa);
    }
}
