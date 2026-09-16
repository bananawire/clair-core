package com.claircore.notifications.application.internal.eventhandlers;

import com.claircore.alerting.interfaces.events.AlertIncidentChangedIntegrationEvent;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.interfaces.acl.AlertDetails;
import com.claircore.notifications.application.commandservices.PushNotificationCommandService;
import com.claircore.notifications.application.internal.outboundservices.acl.ExternalAlertingService;
import com.claircore.notifications.application.internal.outboundservices.acl.ExternalDeviceService;
import com.claircore.notifications.domain.model.commands.SendPushNotificationCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertIncidentChangedEventHandlerTest {

    @Mock
    private ExternalAlertingService externalAlertingService;

    @Mock
    private ExternalDeviceService externalDeviceService;

    @Mock
    private PushNotificationCommandService pushNotificationCommandService;

    @InjectMocks
    private AlertIncidentChangedEventHandler handler;

    private final UUID alertId = UUID.randomUUID();
    private final UUID deviceId = UUID.randomUUID();

    @Test
    void shouldIssueSendCommandWhenAlertIsActiveAndOwnerExists() {
        UUID ownerId = UUID.randomUUID();
        when(externalDeviceService.fetchOwnerIdByDeviceId(deviceId)).thenReturn(Optional.of(ownerId));
        when(externalAlertingService.fetchAlertDetailsById(alertId)).thenReturn(Optional.of(alertDetails("ACTIVE")));
        when(externalDeviceService.fetchDeviceNameByDeviceId(deviceId)).thenReturn(Optional.of("Device 1"));

        handler.on(event(AlertStatus.ACTIVE));

        ArgumentCaptor<SendPushNotificationCommand> captor = ArgumentCaptor.forClass(SendPushNotificationCommand.class);
        verify(pushNotificationCommandService).handle(captor.capture());
        assertEquals(ownerId, captor.getValue().userId());
        assertEquals("Active Alert: Device 1", captor.getValue().title());
        assertEquals("[Device 1] CO2 threshold exceeded", captor.getValue().message());
    }

    @Test
    void shouldSkipWhenDeviceHasNoOwner() {
        when(externalDeviceService.fetchOwnerIdByDeviceId(deviceId)).thenReturn(Optional.empty());

        handler.on(event(AlertStatus.ACTIVE));

        verifyNoInteractions(pushNotificationCommandService);
    }

    @Test
    void shouldIgnoreStatusesOtherThanActiveAndResolved() {
        handler.on(event(AlertStatus.ACKNOWLEDGED));

        verifyNoInteractions(pushNotificationCommandService, externalDeviceService, externalAlertingService);
    }

    private AlertIncidentChangedIntegrationEvent event(AlertStatus status) {
        return new AlertIncidentChangedIntegrationEvent(
                alertId,
                deviceId,
                "HW-01",
                UUID.randomUUID(),
                "CO2",
                BigDecimal.valueOf(800.0),
                BigDecimal.valueOf(1000.0),
                "CO2 threshold exceeded",
                status.name(),
                Instant.now(),
                null);
    }

    private AlertDetails alertDetails(String status) {
        return new AlertDetails(
                alertId,
                deviceId,
                UUID.randomUUID(),
                "Device 1",
                "CO2",
                BigDecimal.valueOf(800.0),
                BigDecimal.valueOf(1000.0),
                "CO2 threshold exceeded",
                status,
                "CRITICAL",
                Instant.now());
    }
}
