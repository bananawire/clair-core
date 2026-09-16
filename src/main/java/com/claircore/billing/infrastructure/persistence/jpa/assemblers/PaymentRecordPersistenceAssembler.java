package com.claircore.billing.infrastructure.persistence.jpa.assemblers;

import com.claircore.billing.domain.model.aggregates.PaymentRecord;
import com.claircore.billing.domain.model.valueobjects.Money;
import com.claircore.billing.infrastructure.persistence.jpa.embeddables.MoneyPersistenceEmbeddable;
import com.claircore.billing.infrastructure.persistence.jpa.entities.PaymentRecordPersistenceEntity;

public final class PaymentRecordPersistenceAssembler {

    private PaymentRecordPersistenceAssembler() {
    }

    public static PaymentRecord toDomainFromPersistence(PaymentRecordPersistenceEntity entity) {
        if (entity == null) return null;
        return PaymentRecord.reconstitute(
                entity.getId(),
                entity.getUserId(),
                toDomain(entity.getAmount()),
                entity.getStatus(),
                entity.getStripePaymentIntentId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static PaymentRecordPersistenceEntity toPersistenceFromDomain(PaymentRecord paymentRecord) {
        if (paymentRecord == null) return null;
        var entity = new PaymentRecordPersistenceEntity();
        entity.setId(paymentRecord.getId());
        entity.setUserId(paymentRecord.getUserId());
        entity.setAmount(toPersistence(paymentRecord.getAmount()));
        entity.setStatus(paymentRecord.getStatus());
        entity.setStripePaymentIntentId(paymentRecord.getStripePaymentIntentId());
        // Null for a record that has never been written; that is what marks the entity as new.
        entity.setCreatedAt(paymentRecord.getCreatedAt());
        entity.setUpdatedAt(paymentRecord.getUpdatedAt());
        return entity;
    }

    private static Money toDomain(MoneyPersistenceEmbeddable e) {
        return e == null ? null : new Money(e.amount(), e.currency());
    }

    private static MoneyPersistenceEmbeddable toPersistence(Money vo) {
        return vo == null ? null : new MoneyPersistenceEmbeddable(vo.amount(), vo.currency());
    }
}
