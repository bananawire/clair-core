package com.claircore.integration;

import com.claircore.billing.application.commandservices.SubscriptionCommandService;
import com.claircore.billing.application.commandservices.UserPlanCommandService;
import com.claircore.billing.domain.model.aggregates.PaymentRecord;
import com.claircore.billing.domain.model.commands.DowngradeToFreemiumCommand;
import com.claircore.billing.domain.model.commands.FulfillSubscriptionCommand;
import com.claircore.billing.domain.model.commands.InitializeUserPlanCommand;
import com.claircore.billing.domain.model.valueobjects.Money;
import com.claircore.billing.domain.model.valueobjects.PlanType;
import com.claircore.billing.domain.model.valueobjects.UserId;
import com.claircore.billing.domain.repositories.PaymentRecordRepository;
import com.claircore.billing.domain.repositories.UserPlanRepository;
import com.claircore.billing.interfaces.acl.BillingContextFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Integración Billing: pago confirmado, upgrade a Premium y downgrade a Freemium")
class DowngradeIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserPlanCommandService userPlanCommandService;
    @Autowired
    private SubscriptionCommandService subscriptionCommandService;
    @Autowired
    private PaymentRecordRepository paymentRecordRepository;
    @Autowired
    private UserPlanRepository userPlanRepository;
    @Autowired
    private BillingContextFacade billingContextFacade;

    @Test
    @DisplayName("Un pago confirmado sube el plan a Premium y los otros contextos ven los límites nuevos")
    void aFulfilledPaymentUpgradesThePlan() {
        // Business / User Story Rational (WS-US-48, WS-US-46): el webhook de Stripe activa Premium y los demás
        // módulos leen los límites del plan a través de la fachada de Billing.
        // Arrange
        UserId user = new UserId(UUID.randomUUID());
        userPlanCommandService.handle(new InitializeUserPlanCommand(user));
        paymentRecordRepository.save(new PaymentRecord(user, new Money(1990L, "usd"), "pi_it_upgrade"));

        // Act
        subscriptionCommandService.handle(new FulfillSubscriptionCommand("pi_it_upgrade", user, new Money(1990L, "usd")));

        // Assert
        assertThat(userPlanRepository.findByUserId(user)).get()
                .extracting(plan -> plan.getPlanType()).isEqualTo(PlanType.PREMIUM);
        assertThat(billingContextFacade.getMaxDevices(user.userId())).isEqualTo(10);
        assertThat(billingContextFacade.canAccessMonthlyReports(user.userId())).isTrue();
    }

    @Test
    @DisplayName("Degradar a Freemium persiste el plan y reduce los límites que ven los otros contextos")
    void downgradingReducesLimits() {
        // Business / User Story Rational (WS-US-47): al vencer o cancelar, el usuario pierde los beneficios Premium.
        // Arrange
        UserId user = new UserId(UUID.randomUUID());
        userPlanCommandService.handle(new InitializeUserPlanCommand(user));
        paymentRecordRepository.save(new PaymentRecord(user, new Money(1990L, "usd"), "pi_it_downgrade"));
        subscriptionCommandService.handle(new FulfillSubscriptionCommand("pi_it_downgrade", user, new Money(1990L, "usd")));

        // Act
        subscriptionCommandService.handle(new DowngradeToFreemiumCommand(user));

        // Assert
        var plan = userPlanRepository.findByUserId(user).orElseThrow();
        assertThat(plan.getPlanType()).isEqualTo(PlanType.FREEMIUM);
        assertThat(plan.getEndDate()).isNull();
        assertThat(billingContextFacade.getMaxDevices(user.userId())).isEqualTo(1);
        assertThat(billingContextFacade.canAccessMonthlyReports(user.userId())).isFalse();
    }

    @Test
    @DisplayName("Degradar a un usuario sin plan registrado es rechazado")
    void downgradingAUserWithoutPlanIsRejected() {
        // Business / User Story Rational (WS-US-47): solo se degrada un plan que existe.
        // Arrange
        UserId unknown = new UserId(UUID.randomUUID());

        // Act + Assert
        assertThatThrownBy(() -> subscriptionCommandService.handle(new DowngradeToFreemiumCommand(unknown)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User plan not found");
    }
}
