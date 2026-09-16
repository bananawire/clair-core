package com.claircore.iam.domain.repositories;

import com.claircore.iam.domain.model.aggregates.RegistrationSession;
import com.claircore.iam.domain.model.valueobjects.RegistrationSessionId;

import java.util.Optional;

/**
 * Port for pending-registration storage. A session expires on its own, so there is no delete for
 * expiry — {@code deleteById} is only for a session that was used or rejected.
 */
public interface RegistrationSessionRepository {

    void save(RegistrationSession session);

    Optional<RegistrationSession> findById(RegistrationSessionId sessionId);

    void deleteById(RegistrationSessionId sessionId);
}
