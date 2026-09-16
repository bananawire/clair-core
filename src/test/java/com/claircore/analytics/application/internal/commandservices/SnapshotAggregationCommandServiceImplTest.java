package com.claircore.analytics.application.internal.commandservices;

import com.claircore.analytics.application.internal.outboundservices.acl.ExternalEvaluationService;
import com.claircore.analytics.domain.model.aggregates.DeviceAnalyticsSnapshot;
import com.claircore.analytics.domain.model.commands.AggregateHourlySnapshotCommand;
import com.claircore.analytics.domain.repositories.DeviceAnalyticsSnapshotRepository;
import com.claircore.analytics.domain.services.AqiCalculator;
import com.claircore.evaluation.interfaces.acl.HourlyTelemetryAverage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SnapshotAggregationCommandServiceImplTest {

    private static final Instant WINDOW_END = Instant.parse("2026-05-16T22:00:00Z");

    @Mock
    private ExternalEvaluationService externalEvaluationService;

    @Mock
    private DeviceAnalyticsSnapshotRepository snapshotRepository;

    private SnapshotAggregationCommandServiceImpl commandService;

    @BeforeEach
    void setUp() {
        commandService = new SnapshotAggregationCommandServiceImpl(
                externalEvaluationService, snapshotRepository, new AqiCalculator());
    }

    @Test
    void snapshotsTheHourThatPrecedesTheWindowEnd() {
        UUID deviceId = UUID.randomUUID();
        when(externalEvaluationService.fetchHourlyTelemetryAggregation(
                Instant.parse("2026-05-16T21:00:00Z"), WINDOW_END))
                .thenReturn(List.of(new HourlyTelemetryAverage(deviceId, 450.0, 12.0, 23.5, 52.0)));

        int written = commandService.handle(new AggregateHourlySnapshotCommand(WINDOW_END));

        assertThat(written).isEqualTo(1);
        var captor = ArgumentCaptor.forClass(DeviceAnalyticsSnapshot.class);
        verify(snapshotRepository).save(captor.capture());
        var snapshot = captor.getValue();
        assertThat(snapshot.getDeviceId().value()).isEqualTo(deviceId);
        assertThat(snapshot.getTimeWindowStart()).isEqualTo(Instant.parse("2026-05-16T21:00:00Z"));
        assertThat(snapshot.getTimeWindowEnd()).isEqualTo(WINDOW_END);
        assertThat(snapshot.getAverageCo2()).isEqualTo(450.0);
        assertThat(snapshot.getCalculatedAqi()).isNotNull();
    }

    @Test
    void writesNothingWhenNoDeviceReportedInTheHour() {
        when(externalEvaluationService.fetchHourlyTelemetryAggregation(
                Instant.parse("2026-05-16T21:00:00Z"), WINDOW_END)).thenReturn(List.of());

        assertThat(commandService.handle(new AggregateHourlySnapshotCommand(WINDOW_END))).isZero();
        verify(snapshotRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsAWindowThatDoesNotSitOnAnHourBoundary() {
        assertThatThrownBy(() -> new AggregateHourlySnapshotCommand(WINDOW_END.plusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hour boundary");
    }
}
