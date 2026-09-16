package com.claircore.billing.application.commandservices;

import com.claircore.billing.domain.model.commands.CreateCheckoutSessionCommand;
import com.claircore.billing.domain.model.commands.CreatePaymentIntentCommand;
import com.claircore.billing.domain.model.commands.DowngradeToFreemiumCommand;
import com.claircore.billing.domain.model.commands.FulfillSubscriptionCommand;

/** Inbound port for the billing write side. */
public interface SubscriptionCommandService {
    String handle(CreateCheckoutSessionCommand command);
    String handle(CreatePaymentIntentCommand command);
    void handle(FulfillSubscriptionCommand command);
    void handle(DowngradeToFreemiumCommand command);
}
