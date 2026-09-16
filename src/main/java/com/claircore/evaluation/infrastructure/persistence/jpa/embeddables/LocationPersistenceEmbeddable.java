package com.claircore.evaluation.infrastructure.persistence.jpa.embeddables;

import jakarta.persistence.Embeddable;

/** Storage shape of {@code Location}; component names are addressed by the entity. */
@Embeddable
public record LocationPersistenceEmbeddable(String country) {
}
