package com.claircore.analytics.infrastructure.persistence.jpa.assemblers;

import com.claircore.analytics.domain.model.aggregates.DeviceAnalyticsSnapshot;
import com.claircore.analytics.domain.model.valueobjects.AirQualityIndex;
import com.claircore.analytics.infrastructure.persistence.jpa.embeddables.AirQualityIndexPersistenceEmbeddable;
import com.claircore.analytics.infrastructure.persistence.jpa.entities.DeviceAnalyticsSnapshotPersistenceEntity;

public final class DeviceAnalyticsSnapshotPersistenceAssembler {

    private DeviceAnalyticsSnapshotPersistenceAssembler() {
    }

    public static DeviceAnalyticsSnapshot toDomainFromPersistence(DeviceAnalyticsSnapshotPersistenceEntity entity) {
        if (entity == null) return null;
        return DeviceAnalyticsSnapshot.reconstitute(
                entity.getId(),
                entity.getDeviceId(),
                entity.getTimeWindowStart(),
                entity.getTimeWindowEnd(),
                entity.getAverageCo2(),
                entity.getAveragePm2_5(),
                entity.getAverageTemperature(),
                entity.getAverageHumidity(),
                new AirQualityIndex(entity.getCalculatedAqi().value(), entity.getCalculatedAqi().category()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static DeviceAnalyticsSnapshotPersistenceEntity toPersistenceFromDomain(DeviceAnalyticsSnapshot snapshot) {
        if (snapshot == null) return null;
        var entity = new DeviceAnalyticsSnapshotPersistenceEntity();
        entity.setId(snapshot.getId());
        entity.setDeviceId(snapshot.getDeviceId());
        entity.setTimeWindowStart(snapshot.getTimeWindowStart());
        entity.setTimeWindowEnd(snapshot.getTimeWindowEnd());
        entity.setAverageCo2(snapshot.getAverageCo2());
        entity.setAveragePm2_5(snapshot.getAveragePm2_5());
        entity.setAverageTemperature(snapshot.getAverageTemperature());
        entity.setAverageHumidity(snapshot.getAverageHumidity());
        entity.setCalculatedAqi(new AirQualityIndexPersistenceEmbeddable(
                snapshot.getCalculatedAqi().value(), snapshot.getCalculatedAqi().category()));
        // Null for a snapshot that has never been written; that is what marks the entity as new.
        entity.setCreatedAt(snapshot.getCreatedAt());
        entity.setUpdatedAt(snapshot.getUpdatedAt());
        return entity;
    }
}
