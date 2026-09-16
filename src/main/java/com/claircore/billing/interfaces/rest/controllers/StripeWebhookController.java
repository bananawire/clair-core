package com.claircore.billing.interfaces.rest.controllers;

import com.claircore.billing.domain.model.commands.FulfillSubscriptionCommand;
import com.claircore.billing.domain.model.valueobjects.Money;
import com.claircore.billing.domain.model.valueobjects.UserId;
import com.claircore.billing.application.commandservices.SubscriptionCommandService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks/stripe")
@Tag(name = "Webhooks", description = "Webhook Endpoints")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;


    private final SubscriptionCommandService subscriptionCommandService;

    public StripeWebhookController(SubscriptionCommandService subscriptionCommandService) {
        this.subscriptionCommandService = subscriptionCommandService;
    }

    @PostMapping
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            log.error("Stripe Webhook Signature Verification Failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        log.info("Received Stripe Webhook event: {}", event.getType());

        if ("payment_intent.succeeded".equals(event.getType())) {
            PaymentIntent paymentIntent = deserializePaymentIntent(event);

            if (paymentIntent == null) {
                log.error("Unable to deserialize PaymentIntent for event id {}. Stripe will retry.", event.getId());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unable to deserialize event");
            }

            var userId = paymentIntent.getMetadata() != null ? paymentIntent.getMetadata().get("userId") : null;
            if (userId == null || userId.isBlank()) {
                log.error("payment_intent.succeeded missing userId metadata. eventId={}, intentId={}", event.getId(), paymentIntent.getId());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing metadata");
            }

                var amount = paymentIntent.getAmount();
                var currency = paymentIntent.getCurrency();

                log.info("Processing payment_intent.succeeded - id: {}, userId: {}, amount: {}", 
                        paymentIntent.getId(), userId, amount);

                try {
                    subscriptionCommandService.handle(new FulfillSubscriptionCommand(
                            paymentIntent.getId(),
                            new UserId(UUID.fromString(userId)),
                            new Money(amount, currency)));
                    log.info("FulfillSubscriptionCommand processed successfully for payment intent id: {}", paymentIntent.getId());
                } catch (Exception e) {
                    // Return 5xx so Stripe retries.
                    log.error("Error handling FulfillSubscriptionCommand", e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Processing failed");
                }
        }

        return ResponseEntity.ok("Received");
    }

    private PaymentIntent deserializePaymentIntent(Event event) {
        var deserializer = event.getDataObjectDeserializer();

        StripeObject stripeObject = deserializer.getObject().orElseGet(() -> {
            try {
                return deserializer.deserializeUnsafe();
            } catch (Exception e) {
                log.error("Stripe event object deserialization failed. eventId={}", event.getId(), e);
                return null;
            }
        });

        return stripeObject instanceof PaymentIntent paymentIntent ? paymentIntent : null;
    }
}
