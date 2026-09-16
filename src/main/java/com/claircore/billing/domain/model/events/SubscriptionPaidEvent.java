package com.claircore.billing.domain.model.events;

import com.claircore.billing.domain.model.valueobjects.UserId;

/**
 * A pending payment has been confirmed. Published by the persistence adapter once the record that
 * registered it has been stored, so a handler that upgrades a plan can trust the payment is durable.
 */
public record SubscriptionPaidEvent(String stripePaymentIntentId, UserId userId) {
    public SubscriptionPaidEvent {
        if (stripePaymentIntentId == null || stripePaymentIntentId.isBlank()) {
            throw new IllegalArgumentException("Stripe Payment Intent ID is required");
        }
        if (userId == null) {
            throw new IllegalArgumentException("UserId is required");
        }
    }
}
