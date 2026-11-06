package com.claircore.evaluation.infrastructure.persistence.jpa.embeddables;

/** Storage shape of {@code Hydrology}; component names are addressed by the entity. */
public record HydrologyPersistenceEmbeddable(Double rainfallMm, Double waterLevelCm) {
}
