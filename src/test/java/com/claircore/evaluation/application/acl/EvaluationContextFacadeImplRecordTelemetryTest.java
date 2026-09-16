package com.claircore.evaluation.application.acl;

import com.claircore.evaluation.application.commandservices.TelemetryEvaluationCommandService;
import com.claircore.evaluation.application.queryservices.TelemetryEvaluationQueryService;
import com.claircore.evaluation.domain.model.aggregates.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.commands.EvaluateTelemetryCommand;
import com.claircore.evaluation.domain.model.valueobjects.AirQuality;
import com.claircore.evaluation.domain.model.valueobjects.Connectivity;
import com.claircore.evaluation.domain.model.valueobjects.DeviceId;
import com.claircore.evaluation.domain.model.valueobjects.Location;
import com.claircore.evaluation.domain.model.valueobjects.ParticulateMatter;
import com.claircore.evaluation.interfaces.acl.TelemetryRecordingResult;
import com.claircore.evaluation.interfaces.acl.TelemetrySubmission;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationContextFacadeImplRecordTelemetryTest {

    @Mock TelemetryEvaluationQueryService queryService;
    @Mock TelemetryEvaluationCommandService commandService;

    @Test
    void recordTelemetryBuildsACommandAndReturnsAcceptedWhenItSucceeds() {
        UUID deviceId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Instant recordedAt = Instant.parse("2026-05-16T22:30:00Z");
        UUID readingId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        TelemetryEvaluation saved = TelemetryEvaluation.reconstitute(
                readingId, new DeviceId(deviceId), readingId, 0L,
                new AirQuality(450.0, 23.0, 50.0),
                new ParticulateMatter(5.0, 12.0, 25.0),
                new Connectivity("ONLINE", "wifi", -50),
                new Location("Peru"),
                95, "OK", recordedAt, recordedAt, recordedAt);
        when(commandService.handle(any(EvaluateTelemetryCommand.class))).thenReturn(saved);

        var facade = new EvaluationContextFacadeImpl(queryService, commandService);
        TelemetryRecordingResult outcome = facade.recordTelemetry(new TelemetrySubmission(
                deviceId, 450.0, 23.0, 50.0, 5.0, 12.0, 25.0, 95, "OK",
                "Peru", "wifi", "ONLINE", -50, recordedAt));

        assertThat(outcome.accepted()).isTrue();
        assertThat(outcome.readingId()).isEqualTo(readingId);
        assertThat(outcome.recordedAt()).isEqualTo(recordedAt);
    }

    @Test
    void recordTelemetryReturnsRejectedForDomainValidationFailures() {
        UUID deviceId = UUID.randomUUID();
        Instant recordedAt = Instant.parse("2026-05-16T22:30:00Z");
        var facade = new EvaluationContextFacadeImpl(queryService, commandService);

        // Negative PM2.5 trips the ParticulateMatter value object constructor.
        TelemetryRecordingResult outcome = facade.recordTelemetry(new TelemetrySubmission(
                deviceId, 800.0, 24.0, 50.0, 1.0, -1.0, 5.0, 95, "OK",
                "Peru", "wifi", "ONLINE", -50, recordedAt));

        assertThat(outcome.accepted()).isFalse();
        assertThat(outcome.readingId()).isNull();
    }

    @Test
    void recordTelemetryCatchesServiceLayerIllegalArgumentAsRejection() {
        UUID deviceId = UUID.randomUUID();
        Instant recordedAt = Instant.parse("2026-05-16T22:30:00Z");
        when(commandService.handle(any(EvaluateTelemetryCommand.class))).thenThrow(new IllegalArgumentException("dup key"));

        var facade = new EvaluationContextFacadeImpl(queryService, commandService);
        TelemetryRecordingResult outcome = facade.recordTelemetry(new TelemetrySubmission(
                deviceId, 800.0, 24.0, 50.0, 1.0, 12.0, 50.0, 95, "OK",
                "Peru", "wifi", "ONLINE", -50, recordedAt));

        assertThat(outcome.accepted()).isFalse();
        assertThat(outcome.readingId()).isNull();
    }
}
