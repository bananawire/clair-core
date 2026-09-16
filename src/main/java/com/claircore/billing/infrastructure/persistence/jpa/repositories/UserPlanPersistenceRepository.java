package com.claircore.billing.infrastructure.persistence.jpa.repositories;

import com.claircore.billing.domain.model.valueobjects.UserId;
import com.claircore.billing.infrastructure.persistence.jpa.entities.UserPlanPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserPlanPersistenceRepository extends JpaRepository<UserPlanPersistenceEntity, UUID> {
    Optional<UserPlanPersistenceEntity> findByUserId(UserId userId);
}
