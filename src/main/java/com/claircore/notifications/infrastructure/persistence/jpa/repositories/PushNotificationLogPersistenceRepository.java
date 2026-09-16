package com.claircore.notifications.infrastructure.persistence.jpa.repositories;

import com.claircore.notifications.infrastructure.persistence.jpa.entities.PushNotificationLogPersistenceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PushNotificationLogPersistenceRepository extends JpaRepository<PushNotificationLogPersistenceEntity, UUID> {
    Page<PushNotificationLogPersistenceEntity> findByUserId(UUID userId, Pageable pageable);
}
