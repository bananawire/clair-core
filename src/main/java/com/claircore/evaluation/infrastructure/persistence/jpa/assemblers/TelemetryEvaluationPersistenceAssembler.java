package com.claircore.evaluation.infrastructure.persistence.jpa.assemblers;

import com.claircore.evaluation.domain.model.aggregates.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.valueobjects.AirQuality;
import com.claircore.evaluation.domain.model.valueobjects.Connectivity;
import com.claircore.evaluation.domain.model.valueobjects.Location;
import com.claircore.evaluation.domain.model.valueobjects.ParticulateMatter;
import com.claircore.evaluation.infrastructure.persistence.jpa.embeddables.AirQualityPersistenceEmbeddable;
import com.claircore.evaluation.infrastructure.persistence.jpa.embeddables.ConnectivityPersistenceEmbeddable;
import com.claircore.evaluation.infrastructure.persistence.jpa.embeddables.LocationPersistenceEmbeddable;
import com.claircore.evaluation.infrastructure.persistence.jpa.embeddables.ParticulateMatterPersistenceEmbeddable;
import com.claircore.evaluation.infrastructure.persistence.jpa.entities.TelemetryEvaluationPersistenceEntity;

public final class TelemetryEvaluationPersistenceAssembler {

    private TelemetryEvaluationPersistenceAssembler() {
    }

    public static TelemetryEvaluation toDomainFromPersistence(TelemetryEvaluationPersistenceEntity entity) {
        if (entity == null) return null;
        return TelemetryEvaluation.reconstitute(
                entity.getId(),
                entity.getDeviceId(),
                entity.getReadingId(),
                entity.getUptime(),
                toDomain(entity.getAirQuality()),
                toDomain(entity.getParticulateMatter()),
                toDomain(entity.getConnectivity()),
                toDomain(entity.getLocation()),
                entity.getHealthStatus(),
                entity.getStatus(),
                entity.getRecordedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static TelemetryEvaluationPersistenceEntity toPersistenceFromDomain(TelemetryEvaluation evaluation) {
        if (evaluation == null) return null;
        var entity = new TelemetryEvaluationPersistenceEntity();
        entity.setId(evaluation.getId());
        entity.setDeviceId(evaluation.getDeviceId());
        entity.setReadingId(evaluation.getReadingId());
        entity.setUptime(evaluation.getUptime());
        entity.setAirQuality(toPersistence(evaluation.getAirQuality()));
        entity.setParticulateMatter(toPersistence(evaluation.getParticulateMatter()));
        entity.setConnectivity(toPersistence(evaluation.getConnectivity()));
        entity.setLocation(toPersistence(evaluation.getLocation()));
        entity.setHealthStatus(evaluation.getHealthStatus());
        entity.setStatus(evaluation.getStatus());
        entity.setRecordedAt(evaluation.getRecordedAt());
        // Null for a reading that has never been written; that is what marks the entity as new.
        entity.setCreatedAt(evaluation.getCreatedAt());
        entity.setUpdatedAt(evaluation.getUpdatedAt());
        return entity;
    }

    private static AirQuality toDomain(AirQualityPersistenceEmbeddable e) {
        return e == null ? null : new AirQuality(e.co2(), e.temperature(), e.humidity());
    }

    private static ParticulateMatter toDomain(ParticulateMatterPersistenceEmbeddable e) {
        return e == null ? null : new ParticulateMatter(e.pm1_0(), e.pm2_5(), e.pm10());
    }

    private static Connectivity toDomain(ConnectivityPersistenceEmbeddable e) {
        return e == null ? null : new Connectivity(e.status(), e.network(), e.signalStrength());
    }

    private static Location toDomain(LocationPersistenceEmbeddable e) {
        return e == null ? null : new Location(e.country());
    }

    private static AirQualityPersistenceEmbeddable toPersistence(AirQuality vo) {
        return vo == null ? null : new AirQualityPersistenceEmbeddable(vo.co2(), vo.temperature(), vo.humidity());
    }

    private static ParticulateMatterPersistenceEmbeddable toPersistence(ParticulateMatter vo) {
        return vo == null ? null : new ParticulateMatterPersistenceEmbeddable(vo.pm1_0(), vo.pm2_5(), vo.pm10());
    }

    private static ConnectivityPersistenceEmbeddable toPersistence(Connectivity vo) {
        return vo == null ? null : new ConnectivityPersistenceEmbeddable(vo.status(), vo.network(), vo.signalStrength());
    }

    private static LocationPersistenceEmbeddable toPersistence(Location vo) {
        return vo == null ? null : new LocationPersistenceEmbeddable(vo.country());
    }
}
