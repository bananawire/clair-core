package com.claircore.analytics.infrastructure.persistence.jpa.embeddables;

import jakarta.persistence.Embeddable;

/** Storage shape of {@code AqiCategoryBreakdown}; component names are addressed by the entities. */
@Embeddable
public record AqiCategoryBreakdownPersistenceEmbeddable(
        long good,
        long moderate,
        long unhealthyForSensitive,
        long unhealthy,
        long veryUnhealthy,
        long hazardous
) {
}
