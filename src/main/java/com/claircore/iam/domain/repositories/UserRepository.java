package com.claircore.iam.domain.repositories;

import com.claircore.iam.domain.model.aggregates.User;
import com.claircore.iam.domain.model.valueobjects.EmailAddress;

import java.util.Optional;

/** Port for user storage. Domain types only. */
public interface UserRepository {

    User save(User user);

    Optional<User> findByEmail(EmailAddress email);

    boolean existsByEmail(EmailAddress email);
}
