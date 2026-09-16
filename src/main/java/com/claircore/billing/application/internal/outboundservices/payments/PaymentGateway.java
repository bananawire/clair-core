package com.claircore.billing.application.internal.outboundservices.payments;

import com.claircore.billing.domain.model.commands.CreateCheckoutSessionCommand;
import com.claircore.billing.domain.model.commands.CreatePaymentIntentCommand;
import com.claircore.billing.domain.model.valueobjects.PaymentIntentResult;

/** Outbound port to the payment provider. Implemented in {@code infrastructure/stripe}. */
public interface PaymentGateway {
    String createCheckoutSession(CreateCheckoutSessionCommand command);
    PaymentIntentResult createPaymentIntent(CreatePaymentIntentCommand command);
}
