package com.claircore.billing.infrastructure.persistence.jpa.repositories;

import com.claircore.billing.domain.model.valueobjects.UserId;
import com.claircore.billing.infrastructure.persistence.jpa.entities.PaymentRecordPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRecordPersistenceRepository extends JpaRepository<PaymentRecordPersistenceEntity, UUID> {
    Optional<PaymentRecordPersistenceEntity> findByStripePaymentIntentId(String stripePaymentIntentId);
    List<PaymentRecordPersistenceEntity> findAllByUserId(UserId userId);
}
