package com.claircore.iam.infrastructure.persistence.jpa.repositories;

import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.infrastructure.persistence.jpa.entities.UserPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPersistenceRepository extends JpaRepository<UserPersistenceEntity, UUID> {

    Optional<UserPersistenceEntity> findByEmail(EmailAddress email);

    boolean existsByEmail(EmailAddress email);
}
