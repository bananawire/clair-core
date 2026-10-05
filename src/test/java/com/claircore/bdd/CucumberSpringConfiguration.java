package com.claircore.bdd;

import com.claircore.billing.application.internal.outboundservices.payments.PaymentGateway;
import com.claircore.iam.application.internal.outboundservices.acl.ExternalNotificationService;
import com.claircore.iam.application.internal.outboundservices.oauth.GoogleTokenExchange;
import com.claircore.iam.application.internal.outboundservices.oauth.GoogleTokenVerifier;
import com.claircore.notifications.application.internal.outboundservices.email.EmailDeliveryService;
import com.claircore.notifications.application.internal.outboundservices.push.PushNotificationDeliveryService;
import com.claircore.testsupport.HermeticHttpTestConfiguration;
import com.claircore.testsupport.InMemoryRegistrationSessionRepository;
import com.claircore.testsupport.InMemoryTokenSessionRepository;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.Mockito.reset;

/**
 * Boots Clair Core on a random port against H2. Sessions stay in memory; Stripe, Google,
 * IAM mail and OneSignal push are mocked.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@Import(HermeticHttpTestConfiguration.class)
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

    @MockitoBean
    EmailDeliveryService emailDeliveryService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    InMemoryTokenSessionRepository tokenSessions;

    @Autowired
    InMemoryRegistrationSessionRepository registrationSessions;

    @Before
    public void resetScenarioState() {
        AbstractCucumberSteps.resetState();
        reset(paymentGateway, googleTokenVerifier, googleTokenExchange,
                externalNotificationService, pushNotificationDeliveryService, emailDeliveryService);
    }

    @After
    public void emptyAllTables() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE'",
                String.class);
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        tables.forEach(table -> jdbcTemplate.execute("TRUNCATE TABLE \"" + table + "\""));
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
        tokenSessions.clear();
        registrationSessions.clear();
    }
}
