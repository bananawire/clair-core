package com.claircore.billing.application.internal.commandservices;

import com.claircore.billing.application.commandservices.SubscriptionCommandService;
import com.claircore.billing.application.internal.outboundservices.payments.PaymentGateway;
import com.claircore.billing.domain.model.aggregates.PaymentRecord;
import com.claircore.billing.domain.model.commands.CreateCheckoutSessionCommand;
import com.claircore.billing.domain.model.commands.CreatePaymentIntentCommand;
import com.claircore.billing.domain.model.commands.DowngradeToFreemiumCommand;
import com.claircore.billing.domain.model.commands.FulfillSubscriptionCommand;
import com.claircore.billing.domain.model.valueobjects.PaymentStatus;
import com.claircore.billing.domain.repositories.PaymentRecordRepository;
import com.claircore.billing.domain.repositories.UserPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionCommandServiceImpl implements SubscriptionCommandService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionCommandServiceImpl.class);

    private final PaymentGateway paymentGateway;
    private final PaymentRecordRepository paymentRecordRepository;
    private final UserPlanRepository userPlanRepository;

    public SubscriptionCommandServiceImpl(PaymentGateway paymentGateway,
                                          PaymentRecordRepository paymentRecordRepository,
                                          UserPlanRepository userPlanRepository) {
        this.paymentGateway = paymentGateway;
        this.paymentRecordRepository = paymentRecordRepository;
        this.userPlanRepository = userPlanRepository;
    }

    @Override
    @Transactional
    public String handle(CreateCheckoutSessionCommand command) {
        return paymentGateway.createCheckoutSession(command);
    }

    @Override
    @Transactional
    public String handle(CreatePaymentIntentCommand command) {
        var result = paymentGateway.createPaymentIntent(command);

        var paymentRecord = new PaymentRecord(
                command.userId(),
                command.money(),
                result.paymentIntentId()
        );
        paymentRecordRepository.save(paymentRecord);

        return result.clientSecret();
    }

    @Override
    @Transactional
    public void handle(FulfillSubscriptionCommand command) {
        log.info("Handling FulfillSubscriptionCommand for paymentIntentId: {}", command.stripePaymentIntentId());

        paymentRecordRepository.findByStripePaymentIntentId(command.stripePaymentIntentId())
                .ifPresentOrElse(
                        paymentRecord -> {
                            if (paymentRecord.getStatus() == PaymentStatus.COMPLETED) {
                                log.info("PaymentRecord with ID: {} is already COMPLETED. Ignoring duplicate webhook.", paymentRecord.getId());
                                return;
                            }

                            log.info("Found paymentRecord with ID: {} in PENDING state. Marking as COMPLETED.", paymentRecord.getId());
                            paymentRecord.markAsCompleted();
                            paymentRecordRepository.save(paymentRecord);
                            log.info("PaymentRecord with ID: {} successfully updated to COMPLETED.", paymentRecord.getId());
                        },
                        () -> log.warn("PaymentRecord not found for stripePaymentIntentId: {}. Cannot mark as completed.",
                                command.stripePaymentIntentId())
                );
    }

    @Override
    @Transactional
    public void handle(DowngradeToFreemiumCommand command) {
        log.info("Handling DowngradeToFreemiumCommand for user: {}", command.userId().userId());
        userPlanRepository.findByUserId(command.userId())
                .ifPresentOrElse(
                        userPlan -> {
                            userPlan.downgradeToFreemium();
                            userPlanRepository.save(userPlan);
                            log.info("User {} successfully downgraded to FREEMIUM.", command.userId().userId());
                        },
                        () -> {
                            log.warn("UserPlan not found for user: {}. Cannot downgrade.", command.userId().userId());
                            throw new IllegalArgumentException("User plan not found");
                        }
                );
    }
}
