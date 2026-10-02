package com.claircore.integration;

import com.claircore.billing.application.internal.outboundservices.payments.PaymentGateway;
import com.claircore.device.application.commandservices.DeviceCommandService;
import com.claircore.device.application.commandservices.OrganizationCommandService;
import com.claircore.device.application.commandservices.SpaceCommandService;
import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.aggregates.Organization;
import com.claircore.device.domain.model.aggregates.Space;
import com.claircore.device.domain.model.commands.ClaimDeviceCommand;
import com.claircore.device.domain.model.commands.CreateOrganizationCommand;
import com.claircore.device.domain.model.commands.CreateSpaceCommand;
import com.claircore.device.domain.model.commands.ImportDevicesCommand;
import com.claircore.device.domain.model.commands.PairDeviceCommand;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.iam.application.internal.outboundservices.acl.AsyncNotificationService;
import com.claircore.iam.application.internal.outboundservices.oauth.GoogleTokenExchange;
import com.claircore.iam.application.internal.outboundservices.oauth.GoogleTokenVerifier;
import com.claircore.iam.domain.repositories.RegistrationSessionRepository;
import com.claircore.iam.domain.repositories.TokenSessionRepository;
import com.claircore.notifications.application.internal.outboundservices.email.EmailDeliveryService;
import com.claircore.notifications.application.internal.outboundservices.push.PushNotificationDeliveryService;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base for the core integration tests: the real Spring context with real services and repositories
 * over H2. Only what leaves the process is mocked (Redis-backed IAM sessions, Stripe, e-mail, push,
 * Google). Deliberately not {@code @Transactional}: the cross-context handlers listen AFTER_COMMIT,
 * so a test that rolled back would never see them run. Tables are emptied after each test instead.
 */
@SpringBootTest
@ActiveProfiles("it")
@Import(AbstractIntegrationTest.NoCacheConfiguration.class)
public abstract class AbstractIntegrationTest {

    private static final AtomicInteger HARDWARE_SEQUENCE = new AtomicInteger(1000);

    @MockitoBean
    protected TokenSessionRepository tokenSessionRepository;
    @MockitoBean
    protected RegistrationSessionRepository registrationSessionRepository;
    @MockitoBean
    protected AsyncNotificationService asyncNotificationService;
    @MockitoBean
    protected PaymentGateway paymentGateway;
    @MockitoBean
    protected EmailDeliveryService emailDeliveryService;
    @MockitoBean
    protected PushNotificationDeliveryService pushNotificationDeliveryService;
    @MockitoBean
    protected GoogleTokenVerifier googleTokenVerifier;
    @MockitoBean
    protected GoogleTokenExchange googleTokenExchange;

    @Autowired
    protected DeviceCommandService deviceCommandService;
    @Autowired
    protected OrganizationCommandService organizationCommandService;
    @Autowired
    protected SpaceCommandService spaceCommandService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** The app caches through Redis; tests read straight through instead. */
    @TestConfiguration
    static class NoCacheConfiguration {
        @Bean
        @Primary
        CacheManager integrationTestCacheManager() {
            return new NoOpCacheManager();
        }
    }

    @AfterEach
    void emptyAllTables() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE'",
                String.class);
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        tables.forEach(table -> jdbcTemplate.execute("TRUNCATE TABLE \"" + table + "\""));
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    // ---- Shared arrange helpers -------------------------------------------------------------

    protected static UserId newUser() {
        return new UserId(UUID.randomUUID());
    }

    protected Organization createOrganization(UserId owner, String name) {
        return organizationCommandService.handle(new CreateOrganizationCommand(name, owner));
    }

    protected Space createSpace(UserId owner, Organization organization, String name) {
        return spaceCommandService.handle(new CreateSpaceCommand(name, organization.getId(), owner));
    }

    /** Puts one unit in the factory inventory and returns its hardware id. */
    protected String registerFactoryDevice(String name) {
        int n = HARDWARE_SEQUENCE.getAndIncrement();
        String hardwareId = "HW-" + n;
        deviceCommandService.handle(new ImportDevicesCommand(List.of(
                new ImportDevicesCommand.DeviceProvisioningRecord("SN-IT-" + n, hardwareId, "api-key-it-" + n, name))));
        return hardwareId;
    }

    /** Factory inventory → pair → claim into {@code space}: a device the user owns. */
    protected DeviceAssignment pairAndClaim(UserId owner, Space space, String name) {
        DeviceAssignment paired = deviceCommandService.handle(new PairDeviceCommand(registerFactoryDevice(name)));
        return deviceCommandService.handle(new ClaimDeviceCommand(paired.getClaimToken().value(), space.getId(), owner));
    }
}
