package com.claircore.billing.infrastructure.persistence.jpa.entities;

import com.claircore.billing.domain.model.valueobjects.PaymentStatus;
import com.claircore.billing.domain.model.valueobjects.UserId;
import com.claircore.billing.infrastructure.persistence.jpa.embeddables.MoneyPersistenceEmbeddable;
import com.claircore.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Storage shape of {@code PaymentRecord}. The table name is stated explicitly: it used to be
 * derived from the aggregate's class name, and the entity is no longer called {@code PaymentRecord}.
 */
@Entity
@Table(name = "payment_record")
public class PaymentRecordPersistenceEntity extends AuditableAbstractPersistenceEntity {

    private UserId userId;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "amount"))
    @AttributeOverride(name = "currency", column = @Column(name = "currency"))
    private MoneyPersistenceEmbeddable amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String stripePaymentIntentId;

    public PaymentRecordPersistenceEntity() {
        // JPA, and the persistence assembler
    }

    public UserId getUserId() { return userId; }
    public void setUserId(UserId userId) { this.userId = userId; }

    public MoneyPersistenceEmbeddable getAmount() { return amount; }
    public void setAmount(MoneyPersistenceEmbeddable amount) { this.amount = amount; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public String getStripePaymentIntentId() { return stripePaymentIntentId; }
    public void setStripePaymentIntentId(String stripePaymentIntentId) { this.stripePaymentIntentId = stripePaymentIntentId; }
}
