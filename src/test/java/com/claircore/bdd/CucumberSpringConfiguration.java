package com.claircore.bdd;

import com.claircore.billing.application.internal.outboundservices.payments.PaymentGateway;
import com.claircore.iam.application.internal.outboundservices.acl.ExternalNotificationService;
import com.claircore.iam.application.internal.outboundservices.oauth.GoogleTokenExchange;
import com.claircore.iam.application.internal.outboundservices.oauth.GoogleTokenVerifier;
import com.claircore.notifications.application.internal.outboundservices.push.PushNotificationDeliveryService;
import io.cucumber.java.Before;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Boots the whole Clair Core application on a random port against PostgreSQL + Redis.
 * Only the systems outside our boundary (Stripe, Google OAuth, IAM mail and OneSignal push) are mocked.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("system")
public class CucumberSpringConfiguration {

    @MockitoBean
    PaymentGateway paymentGateway;

    @MockitoBean
    GoogleTokenVerifier googleTokenVerifier;

    @MockitoBean
    GoogleTokenExchange googleTokenExchange;

    @MockitoBean
    ExternalNotificationService externalNotificationService;

    @MockitoBean
    PushNotificationDeliveryService pushNotificationDeliveryService;

    @Before
    public void resetScenarioState() {
        AbstractCucumberSteps.resetState();
    }
}
