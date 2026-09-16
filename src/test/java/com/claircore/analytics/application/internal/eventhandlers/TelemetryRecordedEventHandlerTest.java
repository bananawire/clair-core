package com.claircore.analytics.application.internal.eventhandlers;

import com.claircore.analytics.application.commandservices.KpiLiveMetricsCommandService;
import com.claircore.analytics.domain.model.commands.ProcessTelemetryAnalyticCommand;
import com.claircore.evaluation.interfaces.events.TelemetryRecordedIntegrationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TelemetryRecordedEventHandlerTest {

    @Mock
    private KpiLiveMetricsCommandService kpiLiveMetricsCommandService;

    @InjectMocks
    private TelemetryRecordedEventHandler handler;

    @Test
    void shouldIssueProcessCommandFromTheIntegrationEvent() {
        UUID deviceId = UUID.randomUUID();
        Instant recordedAt = Instant.parse("2026-05-16T22:30:00Z");

        handler.on(event(deviceId, recordedAt));

        var captor = ArgumentCaptor.forClass(ProcessTelemetryAnalyticCommand.class);
        verify(kpiLiveMetricsCommandService).handle(captor.capture());
        var command = captor.getValue();
        assertEquals(deviceId, command.deviceId().value());
        assertEquals(recordedAt, command.recordedAt());
        assertEquals(450.0, command.co2());
        assertEquals(12.0, command.pm2_5());
        assertEquals(23.5, command.temperature());
        assertEquals(52.0, command.humidity());
    }

    @Test
    void shouldSwallowFailuresSoTheRecordingIsNotRolledBack() {
        doThrow(new IllegalStateException("cache down"))
                .when(kpiLiveMetricsCommandService).handle(any());

        assertDoesNotThrow(() -> handler.on(event(UUID.randomUUID(), Instant.now())));
    }

    private static TelemetryRecordedIntegrationEvent event(UUID deviceId, Instant recordedAt) {
        return new TelemetryRecordedIntegrationEvent(
                deviceId, 450.0, 23.5, 52.0, 5, 12, 25,
                "connected", "Wokwi-GUEST", -65, "PERU", 100, "Optimal", 20L, "14:30:25", recordedAt);
    }
}
