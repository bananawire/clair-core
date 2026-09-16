package com.claircore.analytics.infrastructure.persistence.jpa.assemblers;

import com.claircore.analytics.domain.model.aggregates.DeviceDailySummary;
import com.claircore.analytics.domain.model.valueobjects.AqiCategoryBreakdown;
import com.claircore.analytics.domain.model.valueobjects.MetricStats;
import com.claircore.analytics.infrastructure.persistence.jpa.embeddables.AqiCategoryBreakdownPersistenceEmbeddable;
import com.claircore.analytics.infrastructure.persistence.jpa.embeddables.MetricStatsPersistenceEmbeddable;
import com.claircore.analytics.infrastructure.persistence.jpa.entities.DeviceDailySummaryPersistenceEntity;

public final class DeviceDailySummaryPersistenceAssembler {

    private DeviceDailySummaryPersistenceAssembler() {
    }

    public static DeviceDailySummary toDomainFromPersistence(DeviceDailySummaryPersistenceEntity entity) {
        if (entity == null) return null;
        return DeviceDailySummary.reconstitute(
                entity.getId(),
                entity.getDeviceId(),
                entity.getSummaryDate(),
                toStats(entity.getCo2()),
                toStats(entity.getPm2_5()),
                toStats(entity.getTemperature()),
                toStats(entity.getHumidity()),
                entity.getPeakPm2_5(),
                entity.getPeakPm2_5At(),
                entity.getAverageAqi(),
                entity.getDominantAqiCategory(),
                toBreakdown(entity.getCategoryBreakdown()),
                entity.getReadingCount(),
                entity.getAqiDeltaPct(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static DeviceDailySummaryPersistenceEntity toPersistenceFromDomain(DeviceDailySummary summary) {
        if (summary == null) return null;
        var entity = new DeviceDailySummaryPersistenceEntity();
        entity.setId(summary.getId());
        entity.setDeviceId(summary.getDeviceId());
        entity.setSummaryDate(summary.getSummaryDate());
        entity.setCo2(toStatsEmbeddable(summary.getCo2()));
        entity.setPm2_5(toStatsEmbeddable(summary.getPm2_5()));
        entity.setTemperature(toStatsEmbeddable(summary.getTemperature()));
        entity.setHumidity(toStatsEmbeddable(summary.getHumidity()));
        entity.setPeakPm2_5(summary.getPeakPm2_5());
        entity.setPeakPm2_5At(summary.getPeakPm2_5At());
        entity.setAverageAqi(summary.getAverageAqi());
        entity.setDominantAqiCategory(summary.getDominantAqiCategory());
        entity.setCategoryBreakdown(toBreakdownEmbeddable(summary.getCategoryBreakdown()));
        entity.setReadingCount(summary.getReadingCount());
        entity.setAqiDeltaPct(summary.getAqiDeltaPct());
        // Null for a summary that has never been written; that is what marks the entity as new.
        entity.setCreatedAt(summary.getCreatedAt());
        entity.setUpdatedAt(summary.getUpdatedAt());
        return entity;
    }

    private static MetricStats toStats(MetricStatsPersistenceEmbeddable stats) {
        return stats == null ? null : new MetricStats(stats.avg(), stats.min(), stats.max());
    }

    private static MetricStatsPersistenceEmbeddable toStatsEmbeddable(MetricStats stats) {
        return stats == null ? null : new MetricStatsPersistenceEmbeddable(stats.avg(), stats.min(), stats.max());
    }

    private static AqiCategoryBreakdown toBreakdown(AqiCategoryBreakdownPersistenceEmbeddable b) {
        return b == null ? null : new AqiCategoryBreakdown(
                b.good(), b.moderate(), b.unhealthyForSensitive(), b.unhealthy(), b.veryUnhealthy(), b.hazardous());
    }

    private static AqiCategoryBreakdownPersistenceEmbeddable toBreakdownEmbeddable(AqiCategoryBreakdown b) {
        return b == null ? null : new AqiCategoryBreakdownPersistenceEmbeddable(
                b.good(), b.moderate(), b.unhealthyForSensitive(), b.unhealthy(), b.veryUnhealthy(), b.hazardous());
    }
}
