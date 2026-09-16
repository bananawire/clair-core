package com.claircore.evaluation.infrastructure.persistence.jpa.embeddables;

import jakarta.persistence.Embeddable;

/** Storage shape of {@code Connectivity}; component names are addressed by the entity. */
@Embeddable
public record ConnectivityPersistenceEmbeddable(String status, String network, Integer signalStrength) {
}
