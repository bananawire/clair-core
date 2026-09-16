package com.claircore.evaluation.infrastructure.persistence.jpa.embeddables;

import jakarta.persistence.Embeddable;

/**
 * Storage shape of {@code AirQuality}. Component names must stay as they are: the
 * {@code @AttributeOverride}s on the entity address them by name.
 */
@Embeddable
public record AirQualityPersistenceEmbeddable(Double co2, Double temperature, Double humidity) {
}
