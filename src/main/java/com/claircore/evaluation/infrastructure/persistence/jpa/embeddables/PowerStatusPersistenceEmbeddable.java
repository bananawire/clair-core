package com.claircore.evaluation.infrastructure.persistence.jpa.embeddables;

/** Storage shape of {@code PowerStatus}; component names are addressed by the entity. */
public record PowerStatusPersistenceEmbeddable(Double batteryPercent, Boolean solarCharging) {
}
