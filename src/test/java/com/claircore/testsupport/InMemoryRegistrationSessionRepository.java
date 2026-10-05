package com.claircore.testsupport;

import com.claircore.iam.domain.model.aggregates.RegistrationSession;
import com.claircore.iam.domain.model.valueobjects.RegistrationSessionId;
import com.claircore.iam.domain.repositories.RegistrationSessionRepository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-process stand-in for the Redis registration-session store so sign-up / confirm
 * work without a real Redis.
 */
public final class InMemoryRegistrationSessionRepository implements RegistrationSessionRepository {

    private final ConcurrentHashMap<String, RegistrationSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(RegistrationSession session) {
        sessions.put(session.sessionId().id(), session);
    }

    @Override
    public Optional<RegistrationSession> findById(RegistrationSessionId sessionId) {
        RegistrationSession session = sessions.get(sessionId.id());
        if (session == null || session.isExpired()) {
            if (session != null) {
                sessions.remove(sessionId.id());
            }
            return Optional.empty();
        }
        return Optional.of(session);
    }

    @Override
    public void deleteById(RegistrationSessionId sessionId) {
        sessions.remove(sessionId.id());
    }

    public void clear() {
        sessions.clear();
    }
}
