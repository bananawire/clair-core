package com.claircore.alerting.application.internal.eventhandlers;

import com.claircore.alerting.application.commandservices.AlertCommandService;
import com.claircore.alerting.application.internal.outboundservices.acl.ExternalAlertingEvaluationService;
import com.claircore.alerting.domain.model.commands.EvaluateTelemetryForAlertsCommand;
import com.claircore.evaluation.interfaces.events.TelemetryRecordedIntegrationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class TelemetryRecordedEventHandlerTest {

    @Mock
    private AlertCommandService alertCommandService;
    @Mock
    private ExternalAlertingEvaluationService externalEvaluationService;

    @InjectMocks
    private TelemetryRecordedEventHandler handler;

    @Test
    void shouldEvaluateTelemetryForAlertsWhenEventReceived() {
        UUID deviceId = UUID.randomUUID();
        Instant recordedAt = Instant.parse("2026-05-16T14:30:25Z");

        handler.on(event(deviceId, recordedAt));

        ArgumentCaptor<EvaluateTelemetryForAlertsCommand> captor =
                ArgumentCaptor.forClass(EvaluateTelemetryForAlertsCommand.class);
        verify(alertCommandService).handle(captor.capture());
        EvaluateTelemetryForAlertsCommand command = captor.getValue();

        assertEquals(deviceId, command.deviceId());
        assertEquals(recordedAt, command.occurredAt());
        assertEquals(12.0, command.pm25().doubleValue());
        assertEquals(450.0, command.co2().doubleValue());
        assertEquals(23.5, command.temperature().doubleValue());
        assertEquals(52.0, command.humidity().doubleValue());
        // The receipt is what stops the catch-up scheduler from replaying this reading.
        verify(externalEvaluationService).markAlertsEvaluated(deviceId, UUID.fromString("00000000-0000-0000-0000-000000000123"));
    }

    /** A failing evaluation must not propagate: the publisher is evaluation, not alerting. */
    @Test
    void shouldSwallowFailuresSoThePublishingContextIsNotAffected() {
        doThrow(new IllegalStateException("boom")).when(alertCommandService).handle(any(EvaluateTelemetryForAlertsCommand.class));

        assertDoesNotThrow(() -> handler.on(event(UUID.randomUUID(), Instant.now())));
        // No receipt after a failure, so the reading is replayed instead of lost.
        verify(externalEvaluationService, org.mockito.Mockito.never()).markAlertsEvaluated(any(), any());
    }

    private static TelemetryRecordedIntegrationEvent event(UUID deviceId, Instant recordedAt) {
        return new TelemetryRecordedIntegrationEvent(
                deviceId, 450.0, 23.5, 52.0, 5, 12, 25,
                "connected", "Wokwi-GUEST", -65, "PERU", 100, "Optimal", 20L, "00000000-0000-0000-0000-000000000123", recordedAt);
    }
}
