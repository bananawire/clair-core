package com.claircore.evaluation.infrastructure.persistence.jpa.embeddables;

import jakarta.persistence.Embeddable;

/** Storage shape of {@code ParticulateMatter}; component names are addressed by the entity. */
@Embeddable
public record ParticulateMatterPersistenceEmbeddable(Double pm1_0, Double pm2_5, Double pm10) {
}
