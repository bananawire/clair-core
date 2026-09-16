package com.claircore.notifications.infrastructure.persistence.jpa.assemblers;

import com.claircore.notifications.domain.model.aggregates.PushNotificationLog;
import com.claircore.notifications.infrastructure.persistence.jpa.entities.PushNotificationLogPersistenceEntity;

public final class PushNotificationLogPersistenceAssembler {

    private PushNotificationLogPersistenceAssembler() {
    }

    public static PushNotificationLog toDomainFromPersistence(PushNotificationLogPersistenceEntity entity) {
        if (entity == null) return null;
        return PushNotificationLog.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getAlertId(),
                entity.getTitle(),
                entity.getMessage(),
                entity.isSent(),
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static PushNotificationLogPersistenceEntity toPersistenceFromDomain(PushNotificationLog log) {
        if (log == null) return null;
        var entity = new PushNotificationLogPersistenceEntity();
        entity.setId(log.getId());
        entity.setUserId(log.getUserId());
        entity.setAlertId(log.getAlertId());
        entity.setTitle(log.getTitle());
        entity.setMessage(log.getMessage());
        entity.setSent(log.isSent());
        entity.setErrorMessage(log.getErrorMessage());
        // See EmailLogPersistenceAssembler: null timestamps are what mark the entity as new.
        entity.setCreatedAt(log.getCreatedAt());
        entity.setUpdatedAt(log.getUpdatedAt());
        return entity;
    }
}
