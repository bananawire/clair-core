package com.claircore.analytics.infrastructure.persistence.jpa.embeddables;

import com.claircore.analytics.domain.model.valueobjects.AqiCategory;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Storage shape of {@code AirQualityIndex}. The category stays an enum rather than a String so the
 * generated column keeps its {@code check} constraint.
 */
@Embeddable
public record AirQualityIndexPersistenceEmbeddable(
        Integer value,
        @Enumerated(EnumType.STRING)
        AqiCategory category
) {
}
