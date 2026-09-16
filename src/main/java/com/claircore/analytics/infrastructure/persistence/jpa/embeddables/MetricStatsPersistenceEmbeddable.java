package com.claircore.analytics.infrastructure.persistence.jpa.embeddables;

import jakarta.persistence.Embeddable;

/**
 * Storage shape of {@code MetricStats}. Component names must stay as they are: the
 * {@code @AttributeOverride}s on the entities address them by name, once per metric.
 */
@Embeddable
public record MetricStatsPersistenceEmbeddable(Double avg, Double min, Double max) {
}
