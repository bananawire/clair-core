package com.claircore.billing.infrastructure.stripe;

import com.claircore.billing.application.internal.outboundservices.payments.PaymentGateway;
import com.claircore.billing.domain.model.commands.CreateCheckoutSessionCommand;
import com.claircore.billing.domain.model.commands.CreatePaymentIntentCommand;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import com.claircore.billing.domain.model.valueobjects.PaymentIntentResult;

@Component
public class StripePaymentGatewayAdapter implements PaymentGateway {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    @Override
    public String createCheckoutSession(CreateCheckoutSessionCommand command) {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(command.returnUrl() + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(command.returnUrl())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(command.money().currency())
                                                .setUnitAmount(command.money().amount())
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Clair Core Subscription")
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .putMetadata("userId", command.userId().userId().toString())
                .build();

        try {
            Session session = Session.create(params);
            return session.getUrl();
        } catch (StripeException e) {
            throw new RuntimeException("Error creating Stripe checkout session: " + e.getMessage(), e);
        }
    }
    @Override
    public PaymentIntentResult createPaymentIntent(CreatePaymentIntentCommand command) {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(command.money().amount())
                .setCurrency(command.money().currency())
                .putMetadata("userId", command.userId().userId().toString())
                .build();

        try {
            PaymentIntent intent = PaymentIntent.create(params);
            return new PaymentIntentResult(intent.getId(), intent.getClientSecret());
        } catch (StripeException e) {
            throw new RuntimeException("Error creating Stripe PaymentIntent: " + e.getMessage(), e);
        }
    }
}
