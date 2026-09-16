package com.claircore.alerting.infrastructure.persistence.jpa.repositories;

import com.claircore.alerting.infrastructure.persistence.jpa.entities.AlertTransitionCounterPersistenceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AlertTransitionCounterPersistenceRepository
        extends JpaRepository<AlertTransitionCounterPersistenceEntity, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM AlertTransitionCounterPersistenceEntity c WHERE c.id = :id")
    Optional<AlertTransitionCounterPersistenceEntity> lockById(@Param("id") int id);
}
