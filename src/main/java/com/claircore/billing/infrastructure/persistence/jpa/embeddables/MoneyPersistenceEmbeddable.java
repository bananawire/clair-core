package com.claircore.billing.infrastructure.persistence.jpa.embeddables;

import jakarta.persistence.Embeddable;

/**
 * Storage shape of {@code Money}. Component names must stay as they are: the
 * {@code @AttributeOverride}s on the entity address them by name.
 */
@Embeddable
public record MoneyPersistenceEmbeddable(Long amount, String currency) {
}
