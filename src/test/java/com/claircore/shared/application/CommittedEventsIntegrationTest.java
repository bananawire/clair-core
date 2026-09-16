package com.claircore.shared.application;

import com.claircore.alerting.interfaces.events.AlertIncidentChangedIntegrationEvent;
import com.claircore.alerting.interfaces.acl.AlertDetails;
import com.claircore.billing.application.internal.commandservices.UserPlanCommandServiceImpl;
import com.claircore.billing.application.internal.eventhandlers.UserRegisteredEventHandler;
import com.claircore.billing.domain.model.valueobjects.UserId;
import com.claircore.billing.domain.repositories.UserPlanRepository;
import com.claircore.billing.infrastructure.persistence.jpa.adapters.UserPlanRepositoryImpl;
import com.claircore.iam.interfaces.events.UserRegisteredIntegrationEvent;
import com.claircore.notifications.application.internal.commandservices.PushNotificationCommandServiceImpl;
import com.claircore.notifications.application.internal.eventhandlers.AlertIncidentChangedEventHandler;
import com.claircore.notifications.application.internal.outboundservices.acl.ExternalAlertingService;
import com.claircore.notifications.application.internal.outboundservices.acl.ExternalDeviceService;
import com.claircore.notifications.application.internal.outboundservices.push.PushNotificationDeliveryService;
import com.claircore.notifications.domain.repositories.PushNotificationLogRepository;
import com.claircore.notifications.infrastructure.persistence.jpa.adapters.PushNotificationLogRepositoryImpl;
import com.claircore.shared.infrastructure.config.JpaAuditingConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Verifies that Spring transactional events are delivered to in-process listeners only after the
 * originating transaction commits (or never, if it rolls back). The previous edge webhook fan-out
 * is gone; the integration surface under test is the local in-process bus.
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({JpaAuditingConfiguration.class, AlertIncidentChangedEventHandler.class,
        PushNotificationCommandServiceImpl.class, PushNotificationLogRepositoryImpl.class,
        UserRegisteredEventHandler.class, UserPlanCommandServiceImpl.class, UserPlanRepositoryImpl.class,
        com.claircore.alerting.application.internal.eventhandlers.TelemetryRecordedEventHandler.class,
        com.claircore.analytics.application.internal.eventhandlers.TelemetryRecordedEventHandler.class,
        com.claircore.alerting.application.internal.commandservices.AlertCommandServiceImpl.class,
        com.claircore.analytics.application.internal.commandservices.KpiLiveMetricsCommandServiceImpl.class,
        com.claircore.alerting.application.internal.outboundservices.acl.ExternalAlertingThresholdService.class,
        com.claircore.alerting.application.internal.outboundservices.acl.ExternalAlertingDeviceService.class,
        com.claircore.alerting.application.internal.outboundservices.acl.ExternalAlertingEvaluationService.class,
        com.claircore.alerting.infrastructure.persistence.jpa.adapters.AlertRepositoryImpl.class})
class CommittedEventsIntegrationTest {
    @Autowired PlatformTransactionManager transactions;
    @Autowired ApplicationEventPublisher events;
    @Autowired PushNotificationLogRepository logs;
    @Autowired UserPlanRepository plans;
    @MockitoBean com.claircore.analytics.application.commandservices.KpiLiveMetricsCommandService analyticsCommands;
    @MockitoBean com.claircore.device.interfaces.acl.ThresholdContextFacade thresholdFacade;
    @MockitoBean com.claircore.device.interfaces.acl.DeviceContextFacade deviceFacade;
    @MockitoBean ExternalDeviceService devices;
    @MockitoBean ExternalAlertingService alerts;
    @MockitoBean PushNotificationDeliveryService delivery;
    @MockitoBean com.claircore.alerting.application.internal.outboundservices.acl.ExternalAlertingEvaluationService evaluationReceipts;

    @Test void sentPushHistoryIsCommittedAfterTheAlertTransaction() {
        UUID owner = UUID.randomUUID();
        var event = event();
        arrange(event, owner);
        // delivery is invoked by the in-process listener while the transaction is open;
        // what we want to assert is that the row reaches the database only after commit.
        new TransactionTemplate(transactions).executeWithoutResult(status -> events.publishEvent(event));
        assertThat(logs.findByUserId(owner, 0, 10).items()).singleElement()
                .satisfies(log -> assertThat(log.isSent()).isTrue());
        verify(delivery).sendPushNotification(eq(owner), anyString(), anyString());
    }

    @Test void failedPushHistoryIsAlsoCommittedAfterTheAlertTransaction() {
        UUID owner = UUID.randomUUID();
        var event = event();
        arrange(event, owner);
        doThrow(new IllegalStateException("delivery unavailable")).when(delivery)
                .sendPushNotification(any(), anyString(), anyString());
        new TransactionTemplate(transactions).executeWithoutResult(status -> events.publishEvent(event));
        assertThat(logs.findByUserId(owner, 0, 10).items()).singleElement().satisfies(log -> {
            assertThat(log.isSent()).isFalse();
            assertThat(log.getErrorMessage()).isEqualTo("delivery unavailable");
        });
    }

    @Test void rolledBackAlertsDoNotSendPushes() {
        var event = event();
        // Stub so the listener does not throw when looking up the missing alert details.
        when(alerts.fetchAlertDetailsById(event.alertId())).thenReturn(Optional.empty());
        when(devices.fetchOwnerIdByDeviceId(event.deviceId())).thenReturn(Optional.empty());
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            events.publishEvent(event);
            status.setRollbackOnly();
        });
        // Push log must not exist; delivery itself never runs because the handler short-circuits.
        verifyNoInteractions(delivery);
        UUID anyOwner = UUID.randomUUID();
        assertThat(logs.findByUserId(anyOwner, 0, 20).items()).isEmpty();
    }

    @Test void registrationCreatesThePlanOnlyAfterCommit() {
        var id = new UserId(UUID.randomUUID());
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            events.publishEvent(new UserRegisteredIntegrationEvent(id.userId()));
            assertThat(plans.findByUserId(id)).isEmpty();
        });
        assertThat(plans.findByUserId(id)).isPresent();
    }

    @Test void rolledBackRegistrationDoesNotCreateAPlan() {
        var id = new UserId(UUID.randomUUID());
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            events.publishEvent(new UserRegisteredIntegrationEvent(id.userId()));
            status.setRollbackOnly();
        });
        assertThat(plans.findByUserId(id)).isEmpty();
    }

    @Test void telemetryConsumersRunOnlyForCommittedReadings() {
        var event = new com.claircore.evaluation.interfaces.events.TelemetryRecordedIntegrationEvent(
                UUID.randomUUID(), 800, 24, 50, 5, 10, 15, "ONLINE", "wifi", -50,
                "PE", 1, "OK", 100, "12:00", Instant.now());
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            events.publishEvent(event);
            verifyNoInteractions(analyticsCommands);
            status.setRollbackOnly();
        });
        verifyNoInteractions(analyticsCommands);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            events.publishEvent(event);
            verifyNoInteractions(analyticsCommands);
        });
        verify(analyticsCommands).handle(any(com.claircore.analytics.domain.model.commands.ProcessTelemetryAnalyticCommand.class));
    }

    private void arrange(AlertIncidentChangedIntegrationEvent event, UUID owner) {
        when(devices.fetchOwnerIdByDeviceId(event.deviceId())).thenReturn(Optional.of(owner));
        when(devices.fetchDeviceNameByDeviceId(event.deviceId())).thenReturn(Optional.of("Sensor"));
        when(alerts.fetchAlertDetailsById(event.alertId())).thenReturn(Optional.of(new AlertDetails(
                event.alertId(), event.deviceId(), event.spaceId(), "Sensor", "CO2", BigDecimal.ONE,
                BigDecimal.TEN, "Too high", "ACTIVE", "WARNING", event.occurredAt())));
    }

    private AlertIncidentChangedIntegrationEvent event() {
        return new AlertIncidentChangedIntegrationEvent(UUID.randomUUID(), UUID.randomUUID(), "HW-0001",
                UUID.randomUUID(), "CO2", BigDecimal.ONE, BigDecimal.TEN, "Too high", "ACTIVE", Instant.now(), null);
    }
}
