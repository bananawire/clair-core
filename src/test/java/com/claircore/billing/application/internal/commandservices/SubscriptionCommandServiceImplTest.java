package com.claircore.billing.application.internal.commandservices;

import com.claircore.billing.application.internal.outboundservices.payments.PaymentGateway;
import com.claircore.billing.domain.model.aggregates.PaymentRecord;
import com.claircore.billing.domain.model.commands.CreatePaymentIntentCommand;
import com.claircore.billing.domain.model.commands.FulfillSubscriptionCommand;
import com.claircore.billing.domain.model.valueobjects.Money;
import com.claircore.billing.domain.model.valueobjects.PaymentIntentResult;
import com.claircore.billing.domain.model.valueobjects.PaymentStatus;
import com.claircore.billing.domain.model.valueobjects.UserId;
import com.claircore.billing.domain.repositories.PaymentRecordRepository;
import com.claircore.billing.domain.repositories.UserPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionCommandServiceImplTest {

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private PaymentRecordRepository paymentRecordRepository;

    @Mock
    private UserPlanRepository userPlanRepository;

    @InjectMocks
    private SubscriptionCommandServiceImpl service;

    @Test
    void shouldCreatePaymentRecordWhenPaymentIntentIsCreated() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440020");
        CreatePaymentIntentCommand command = new CreatePaymentIntentCommand(new UserId(userId), new Money(1500L, "usd"));
        when(paymentGateway.createPaymentIntent(command)).thenReturn(new PaymentIntentResult("pi_123", "cs_123"));

        String clientSecret = service.handle(command);

        assertThat(clientSecret).isEqualTo("cs_123");
        ArgumentCaptor<PaymentRecord> captor = ArgumentCaptor.forClass(PaymentRecord.class);
        verify(paymentRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(new UserId(userId));
        assertThat(captor.getValue().getAmount()).isEqualTo(new Money(1500L, "usd"));
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(captor.getValue().getStripePaymentIntentId()).isEqualTo("pi_123");
    }

    @Test
    void shouldCompletePaymentRecordWhenWebhookIsProcessedForPendingPayment() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440021");
        PaymentRecord paymentRecord = new PaymentRecord(new UserId(userId), new Money(1500L, "usd"), "pi_456");
        when(paymentRecordRepository.findByStripePaymentIntentId("pi_456")).thenReturn(Optional.of(paymentRecord));

        service.handle(new FulfillSubscriptionCommand("pi_456", new UserId(userId), new Money(1500L, "usd")));

        assertThat(paymentRecord.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        verify(paymentRecordRepository).save(paymentRecord);
    }

    @Test
    void shouldIgnoreWebhookRetriesWhenPaymentWasAlreadyCompleted() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440022");
        PaymentRecord paymentRecord = new PaymentRecord(new UserId(userId), new Money(1500L, "usd"), "pi_789");
        paymentRecord.markAsCompleted();
        when(paymentRecordRepository.findByStripePaymentIntentId("pi_789")).thenReturn(Optional.of(paymentRecord));

        assertDoesNotThrow(() -> service.handle(new FulfillSubscriptionCommand("pi_789", new UserId(userId), new Money(1500L, "usd"))));

        assertThat(paymentRecord.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        verify(paymentRecordRepository, never()).save(any());
    }

    @Test
    void shouldNotPersistWhenPaymentRecordDoesNotExist() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440023");
        when(paymentRecordRepository.findByStripePaymentIntentId("pi_missing")).thenReturn(Optional.empty());

        service.handle(new FulfillSubscriptionCommand("pi_missing", new UserId(userId), new Money(1500L, "usd")));

        verify(paymentRecordRepository, never()).save(any());
    }
}
