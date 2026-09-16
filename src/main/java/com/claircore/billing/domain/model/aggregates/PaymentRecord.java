package com.claircore.billing.domain.model.aggregates;

import com.claircore.billing.domain.model.events.SubscriptionPaidEvent;
import com.claircore.billing.domain.model.valueobjects.Money;
import com.claircore.billing.domain.model.valueobjects.PaymentStatus;
import com.claircore.billing.domain.model.valueobjects.UserId;
import com.claircore.shared.domain.model.aggregates.AbstractDomainAggregateRoot;

import java.time.Instant;
import java.util.UUID;

/** One attempted payment, from the intent that created it to the confirmation that settles it. */
public class PaymentRecord extends AbstractDomainAggregateRoot {

    private final UUID id;
    private final UserId userId;
    private final Money amount;
    private PaymentStatus status;
    private final String stripePaymentIntentId;
    private final Instant createdAt;
    private final Instant updatedAt;

    private PaymentRecord(UUID id, UserId userId, Money amount, PaymentStatus status,
                          String stripePaymentIntentId, Instant createdAt, Instant updatedAt) {
        if (id == null) {
            throw new IllegalArgumentException("Id must not be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
        this.stripePaymentIntentId = stripePaymentIntentId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public PaymentRecord(UserId userId, Money amount, String stripePaymentIntentId) {
        this(UUID.randomUUID(), userId, amount, PaymentStatus.PENDING, stripePaymentIntentId, null, null);
    }

    /** Rebuilds a record that already exists in storage, identity and audit timestamps included. */
    public static PaymentRecord reconstitute(UUID id, UserId userId, Money amount, PaymentStatus status,
                                             String stripePaymentIntentId, Instant createdAt, Instant updatedAt) {
        return new PaymentRecord(id, userId, amount, status, stripePaymentIntentId, createdAt, updatedAt);
    }

    public void markAsCompleted() {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("PaymentRecord can only be completed from PENDING status");
        }
        this.status = PaymentStatus.COMPLETED;
        registerEvent(new SubscriptionPaidEvent(this.stripePaymentIntentId, this.userId));
    }

    public UUID getId() { return id; }
    public UserId getUserId() { return userId; }
    public Money getAmount() { return amount; }
    public PaymentStatus getStatus() { return status; }
    public String getStripePaymentIntentId() { return stripePaymentIntentId; }

    /** Null until the record has been written; assigned by persistence auditing. */
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
