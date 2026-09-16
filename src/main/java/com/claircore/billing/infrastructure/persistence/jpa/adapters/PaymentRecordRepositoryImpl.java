package com.claircore.billing.infrastructure.persistence.jpa.adapters;

import com.claircore.billing.domain.model.aggregates.PaymentRecord;
import com.claircore.billing.domain.model.valueobjects.UserId;
import com.claircore.billing.domain.repositories.PaymentRecordRepository;
import com.claircore.billing.infrastructure.persistence.jpa.assemblers.PaymentRecordPersistenceAssembler;
import com.claircore.billing.infrastructure.persistence.jpa.repositories.PaymentRecordPersistenceRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PaymentRecordRepositoryImpl implements PaymentRecordRepository {

    private final PaymentRecordPersistenceRepository paymentRecordPersistenceRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentRecordRepositoryImpl(PaymentRecordPersistenceRepository paymentRecordPersistenceRepository,
                                       ApplicationEventPublisher eventPublisher) {
        this.paymentRecordPersistenceRepository = paymentRecordPersistenceRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Publishes the aggregate's events after the write, the job Spring Data's
     * {@code AbstractAggregateRoot} used to do. It cannot do it any more: the instance it is handed
     * is the persistence entity, and the events are registered on the aggregate.
     */
    @Override
    public PaymentRecord save(PaymentRecord paymentRecord) {
        var saved = paymentRecordPersistenceRepository.save(
                PaymentRecordPersistenceAssembler.toPersistenceFromDomain(paymentRecord));
        var domain = PaymentRecordPersistenceAssembler.toDomainFromPersistence(saved);

        paymentRecord.domainEvents().forEach(eventPublisher::publishEvent);
        paymentRecord.clearDomainEvents();

        return domain;
    }

    @Override
    public Optional<PaymentRecord> findById(UUID id) {
        return paymentRecordPersistenceRepository.findById(id)
                .map(PaymentRecordPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<PaymentRecord> findByStripePaymentIntentId(String stripePaymentIntentId) {
        return paymentRecordPersistenceRepository.findByStripePaymentIntentId(stripePaymentIntentId)
                .map(PaymentRecordPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<PaymentRecord> findAllByUserId(UserId userId) {
        return paymentRecordPersistenceRepository.findAllByUserId(userId).stream()
                .map(PaymentRecordPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }
}
