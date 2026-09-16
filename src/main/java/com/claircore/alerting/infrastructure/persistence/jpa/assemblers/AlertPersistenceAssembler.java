package com.claircore.alerting.infrastructure.persistence.jpa.assemblers;

import com.claircore.alerting.domain.model.aggregates.Alert;
import com.claircore.alerting.infrastructure.persistence.jpa.entities.AlertPersistenceEntity;

public final class AlertPersistenceAssembler {

    private AlertPersistenceAssembler() {
    }

    public static Alert toDomainFromPersistence(AlertPersistenceEntity entity) {
        if (entity == null) return null;
        return Alert.reconstitute(
                entity.getId(),
                entity.getDeviceId(),
                entity.getSpaceId(),
                entity.getSpaceName(),
                entity.getDeviceName(),
                entity.getMetric(),
                entity.getThresholdValue(),
                entity.getActualValue(),
                entity.getMessage(),
                entity.getStatus(),
                entity.getSeverity(),
                entity.getOccurredAt(),
                entity.getResolvedAt(),
                entity.getTransitionSequence(),
                entity.getEdgeReceiptSequence(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static AlertPersistenceEntity toPersistenceFromDomain(Alert alert) {
        if (alert == null) return null;
        var entity = new AlertPersistenceEntity();
        entity.setId(alert.getId());
        entity.setDeviceId(alert.getDeviceId());
        entity.setSpaceId(alert.getSpaceId());
        entity.setSpaceName(alert.getSpaceName());
        entity.setDeviceName(alert.getDeviceName());
        entity.setMetric(alert.getMetric());
        entity.setThresholdValue(alert.getThresholdValue());
        entity.setActualValue(alert.getActualValue());
        entity.setMessage(alert.getMessage());
        entity.setStatus(alert.getStatus());
        entity.setSeverity(alert.getSeverity());
        entity.setOccurredAt(alert.getOccurredAt());
        entity.setResolvedAt(alert.getResolvedAt());
        entity.setTransitionSequence(alert.getTransitionSequence());
        entity.setEdgeReceiptSequence(alert.getEdgeReceiptSequence());
        // Null for an alert that has never been written; that is what marks the entity as new.
        entity.setCreatedAt(alert.getCreatedAt());
        entity.setUpdatedAt(alert.getUpdatedAt());
        return entity;
    }
}
