package com.claircore.billing.interfaces.rest.controllers;

import com.claircore.billing.domain.model.commands.CreateCheckoutSessionCommand;
import com.claircore.billing.domain.model.commands.CreatePaymentIntentCommand;
import com.claircore.billing.domain.model.commands.DowngradeToFreemiumCommand;
import com.claircore.billing.domain.model.queries.GetSubscriptionsByUserIdQuery;
import com.claircore.billing.domain.model.queries.GetUserPlanQuery;
import com.claircore.billing.domain.model.valueobjects.Money;
import com.claircore.billing.domain.model.valueobjects.PaymentStatus;
import com.claircore.billing.domain.model.valueobjects.UserId;
import com.claircore.billing.application.commandservices.SubscriptionCommandService;
import com.claircore.billing.application.queryservices.SubscriptionQueryService;
import com.claircore.billing.interfaces.rest.resources.CreateSubscriptionResource;
import com.claircore.billing.interfaces.rest.resources.SubscriptionResource;
import com.claircore.billing.interfaces.rest.resources.UserPlanResource;
import com.claircore.billing.interfaces.rest.transform.SubscriptionResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/subscriptions")
@Tag(name = "Subscriptions", description = "Billing and Subscription Management Endpoints")
public class SubscriptionController {

    private final SubscriptionCommandService subscriptionCommandService;
    private final SubscriptionQueryService subscriptionQueryService;

    public SubscriptionController(SubscriptionCommandService subscriptionCommandService, SubscriptionQueryService subscriptionQueryService) {
        this.subscriptionCommandService = subscriptionCommandService;
        this.subscriptionQueryService = subscriptionQueryService;
    }

    @PostMapping("/checkout-session")
    @Operation(summary = "Create a Stripe checkout session")
    public ResponseEntity<Map<String, String>> createCheckoutSession(@RequestBody CreateSubscriptionResource resource) {
        var command = new CreateCheckoutSessionCommand(
                new UserId(UUID.fromString(resource.userId())),
                new Money(resource.amount(), resource.currency()),
                resource.returnUrl()
        );
        String sessionUrl = subscriptionCommandService.handle(command);
        return ResponseEntity.ok(Map.of("checkoutUrl", sessionUrl));
    }

    @PostMapping("/payment-intent")
    @Operation(summary = "Create a Stripe payment intent")
    public ResponseEntity<Map<String, String>> createPaymentIntent(@RequestBody CreateSubscriptionResource resource) {
        var command = new CreatePaymentIntentCommand(
                new UserId(UUID.fromString(resource.userId())),
                new Money(resource.amount(), resource.currency())
        );
        String clientSecret = subscriptionCommandService.handle(command);
        return ResponseEntity.ok(Map.of("clientSecret", clientSecret));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all subscriptions for a user")
    public ResponseEntity<List<SubscriptionResource>> getSubscriptionsByUserId(@PathVariable String userId) {
        var query = new GetSubscriptionsByUserIdQuery(new UserId(UUID.fromString(userId)));
        var subscriptions = subscriptionQueryService.handle(query);
        var resources = subscriptions.stream()
                .map(SubscriptionResourceFromEntityAssembler::toResourceFromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/plans/{userId}")
    @Operation(summary = "Get current plan (premium or freemium) for a user")
    public ResponseEntity<UserPlanResource> getUserPlan(@PathVariable String userId) {
        var plan = subscriptionQueryService.resolveUserPlan(new GetUserPlanQuery(userId));
        var activeStatus = subscriptionQueryService
                .handle(new GetSubscriptionsByUserIdQuery(new UserId(UUID.fromString(userId))))
                .stream()
                .filter(s -> s.getStatus() == PaymentStatus.COMPLETED)
                .findFirst()
                .map(s -> s.getStatus().name())
                .orElse(null);
        return ResponseEntity.ok(new UserPlanResource(userId, plan, activeStatus));
    }

    @PostMapping("/downgrade/{userId}")
    @Operation(summary = "Downgrade a user's plan to FREEMIUM")
    public ResponseEntity<Void> downgradeToFreemium(@PathVariable String userId) {
        subscriptionCommandService.handle(new DowngradeToFreemiumCommand(new UserId(UUID.fromString(userId))));
        return ResponseEntity.ok().build();
    }
}
