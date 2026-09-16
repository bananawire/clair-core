package com.claircore.evaluation.application.internal.commandservices;

import com.claircore.evaluation.domain.model.commands.EvaluateTelemetryCommand;
import com.claircore.evaluation.domain.model.aggregates.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.valueobjects.*;
import com.claircore.evaluation.domain.repositories.TelemetryEvaluationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryEvaluationCommandServiceImplTest {

    @Mock
    private TelemetryEvaluationRepository telemetryEvaluationRepository;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TelemetryEvaluationCommandServiceImpl telemetryEvaluationCommandService;

    @Test
    void shouldSaveAndReturnTelemetryEvaluationWhenCommandIsValid() {
        // Arrange
        var command = new EvaluateTelemetryCommand(
                new DeviceId(UUID.randomUUID()),
                UUID.fromString("00000000-0000-0000-0000-000000000123"),
                3600L,
                new AirQuality(400.0, 22.0, 45.0),
                new ParticulateMatter(10.0, 15.0, 25.0),
                new Connectivity("ONLINE", "WiFi", -50),
                new Location("Chile"),
                85,
                "STABLE",
                Instant.now()
        );

        TelemetryEvaluation expectedEvaluation = new TelemetryEvaluation(
                command.deviceId(), command.readingId(), command.uptime(),
                command.airQuality(), command.particulateMatter(),
                command.connectivity(), command.location(),
                command.healthStatus(), command.status(), command.recordedAt()
        );

        when(telemetryEvaluationRepository.saveIfAbsent(any(TelemetryEvaluation.class))).thenReturn(new TelemetryEvaluationRepository.StoredReading(expectedEvaluation, true));

        // Act
        TelemetryEvaluation result = telemetryEvaluationCommandService.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDeviceId()).isEqualTo(command.deviceId());
        assertThat(result.getHealthStatus()).isEqualTo(command.healthStatus());
        verify(telemetryEvaluationRepository).saveIfAbsent(any(TelemetryEvaluation.class));
    }
    @Test
    void replayRepublishesOnlyReadingsWithoutAReceipt() {
        var reading = new TelemetryEvaluation(
                new DeviceId(UUID.randomUUID()), UUID.randomUUID(), 10L,
                new AirQuality(400.0, 22.0, 45.0), new ParticulateMatter(1.0, 2.0, 3.0),
                new Connectivity("ONLINE", "WiFi", -50), new Location("Chile"), 100, "STABLE", Instant.now());
        Instant before = Instant.now().minusSeconds(30);
        when(telemetryEvaluationRepository.findAlertsPending(before, 200)).thenReturn(java.util.List.of(reading));
        int replayed = telemetryEvaluationCommandService.handle(
                new com.claircore.evaluation.domain.model.commands.ReplayUnprocessedTelemetryCommand(before, 200));
        assertThat(replayed).isEqualTo(1);
        var captor = org.mockito.ArgumentCaptor.forClass(com.claircore.evaluation.interfaces.events.TelemetryRecordedIntegrationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().readingId()).isEqualTo(reading.getReadingId().toString());
        telemetryEvaluationCommandService.handle(
                new com.claircore.evaluation.domain.model.commands.MarkAlertsEvaluatedCommand(reading.getDeviceId().value(), reading.getReadingId(), before));
        verify(telemetryEvaluationRepository).markAlertsEvaluated(reading.getDeviceId().value(), reading.getReadingId(), before);
    }
}
