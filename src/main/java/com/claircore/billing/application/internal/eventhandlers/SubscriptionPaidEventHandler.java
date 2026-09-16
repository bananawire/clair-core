package com.claircore.billing.application.internal.eventhandlers;

import com.claircore.billing.domain.model.events.SubscriptionPaidEvent;
import com.claircore.billing.domain.repositories.UserPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionPaidEventHandler {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionPaidEventHandler.class);
    private final UserPlanRepository userPlanRepository;

    public SubscriptionPaidEventHandler(UserPlanRepository userPlanRepository) {
        this.userPlanRepository = userPlanRepository;
    }

    @EventListener
    public void on(SubscriptionPaidEvent event) {
        log.info("Received SubscriptionPaidEvent for payment intent: {}, upgrading user {}",
            event.stripePaymentIntentId(), event.userId().userId());

        userPlanRepository.findByUserId(event.userId())
            .ifPresentOrElse(
                userPlan -> {
                    userPlan.upgradeToPremium();
                    userPlanRepository.save(userPlan);
                    log.info("Successfully upgraded user {} to PREMIUM plan", event.userId().userId());
                },
                () -> log.warn("UserPlan not found for user {}. Cannot upgrade to PREMIUM.", event.userId().userId())
            );
    }
}
