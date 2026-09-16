package com.claircore.billing.infrastructure.persistence.jpa.adapters;

import com.claircore.billing.domain.model.aggregates.PaymentRecord;
import com.claircore.billing.domain.model.events.SubscriptionPaidEvent;
import com.claircore.billing.domain.model.valueobjects.Money;
import com.claircore.billing.domain.model.valueobjects.PaymentStatus;
import com.claircore.billing.domain.model.valueobjects.UserId;
import com.claircore.billing.domain.repositories.PaymentRecordRepository;
import com.claircore.shared.infrastructure.config.JpaAuditingConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the port through its adapter, and pins the one behaviour the split could silently lose:
 * Spring Data used to publish an aggregate's events when it was saved, but the instance it now sees
 * is the persistence entity, so the adapter has to drain them itself.
 */
@DataJpaTest
@Import({JpaAuditingConfiguration.class, PaymentRecordRepositoryImpl.class,
        PaymentRecordRepositoryImplTest.RecordingSubscriber.class})
class PaymentRecordRepositoryImplTest {

    @Autowired
    private PaymentRecordRepository repository;

    @Autowired
    private RecordingSubscriber subscriber;

    @BeforeEach
    void resetSubscriber() {
        subscriber.received.clear();
    }

    @Test
    void savesWithTheIdentityTheAggregateAssignedAndFillsTheAuditTimestamps() {
        var record = new PaymentRecord(new UserId(UUID.randomUUID()), new Money(1500L, "usd"), "pi_new");

        var saved = repository.save(record);

        assertThat(saved.getId()).isEqualTo(record.getId());
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void roundTripsEveryFieldThroughStorage() {
        UUID userId = UUID.randomUUID();
        repository.save(new PaymentRecord(new UserId(userId), new Money(2500L, "eur"), "pi_round"));

        var found = repository.findByStripePaymentIntentId("pi_round").orElseThrow();

        assertThat(found.getUserId()).isEqualTo(new UserId(userId));
        assertThat(found.getAmount()).isEqualTo(new Money(2500L, "eur"));
        assertThat(found.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(found.getStripePaymentIntentId()).isEqualTo("pi_round");
        assertThat(repository.findAllByUserId(new UserId(userId))).hasSize(1);
        assertThat(repository.findById(found.getId())).isPresent();
    }

    @Test
    void publishesTheEventsTheAggregateRegisteredAndDrainsThem() {
        UUID userId = UUID.randomUUID();
        var record = repository.save(
                new PaymentRecord(new UserId(userId), new Money(1500L, "usd"), "pi_paid"));
        record.markAsCompleted();

        repository.save(record);

        assertThat(subscriber.received)
                .containsExactly(new SubscriptionPaidEvent("pi_paid", new UserId(userId)));
        assertThat(record.domainEvents()).isEmpty();
        assertThat(repository.findByStripePaymentIntentId("pi_paid").orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void publishesNothingWhenTheAggregateRegisteredNoEvents() {
        repository.save(new PaymentRecord(new UserId(UUID.randomUUID()), new Money(100L, "usd"), "pi_quiet"));

        assertThat(subscriber.received).isEmpty();
    }

    @Component
    static class RecordingSubscriber {
        private final List<SubscriptionPaidEvent> received = new ArrayList<>();

        @EventListener
        void on(SubscriptionPaidEvent event) {
            received.add(event);
        }
    }
}
