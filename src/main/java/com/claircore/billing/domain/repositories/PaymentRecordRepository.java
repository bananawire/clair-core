package com.claircore.billing.domain.repositories;

import com.claircore.billing.domain.model.aggregates.PaymentRecord;
import com.claircore.billing.domain.model.valueobjects.UserId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port for payment record storage. Domain types only. */
public interface PaymentRecordRepository {

    PaymentRecord save(PaymentRecord paymentRecord);

    Optional<PaymentRecord> findById(UUID id);

    Optional<PaymentRecord> findByStripePaymentIntentId(String stripePaymentIntentId);

    List<PaymentRecord> findAllByUserId(UserId userId);
}
