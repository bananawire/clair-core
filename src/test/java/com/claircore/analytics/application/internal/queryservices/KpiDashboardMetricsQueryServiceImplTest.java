package com.claircore.analytics.application.internal.queryservices;

import com.claircore.analytics.application.internal.outboundservices.cache.KpiLiveMetricsBuffer;
import com.claircore.analytics.application.internal.outboundservices.cache.LiveMetricsStore;
import com.claircore.shared.domain.exceptions.ResourceNotFoundException;
import com.claircore.analytics.domain.model.aggregates.DeviceAnalyticsSnapshot;
import com.claircore.analytics.domain.model.queries.GetDashboardMetricsQuery;
import com.claircore.analytics.domain.model.valueobjects.AirQualityIndex;
import com.claircore.analytics.domain.model.valueobjects.AqiCategory;
import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.domain.model.valueobjects.MetricAverages;
import com.claircore.analytics.domain.model.valueobjects.TrendPeriod;
import com.claircore.analytics.domain.repositories.DeviceAnalyticsSnapshotRepository;
import com.claircore.analytics.domain.services.AqiCalculator;
import com.claircore.analytics.domain.services.TrendAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KpiDashboardMetricsQueryServiceImplTest {

    private final UUID deviceId = UUID.randomUUID();

    @Mock
    private LiveMetricsStore liveMetricsStore;

    @Mock
    private DeviceAnalyticsSnapshotRepository snapshotRepository;

    @Mock private com.claircore.analytics.application.internal.outboundservices.acl.ExternalEvaluationService evaluations;

    private KpiDashboardMetricsQueryServiceImpl queryService;

    @BeforeEach
    void setUp() {
        queryService = new KpiDashboardMetricsQueryServiceImpl(
                liveMetricsStore,
                new AqiCalculator(),
                new TrendAnalyzer(),
                snapshotRepository, evaluations);
    }

    @Test
    void aNullPeriodWithNoWindowReadsTheLiveBuffer() {
        when(liveMetricsStore.getIfPresent(deviceId)).thenReturn(bufferWithCo2(450.0));
        when(snapshotRepository.findLatestByDeviceId(deviceId)).thenReturn(Optional.empty());

        var metrics = queryService.handle(query(null, null, null)).orElseThrow();

        assertThat(metrics.averageCo2()).isCloseTo(450.0, within(0.001));
        assertThat(metrics.averagePm2_5()).isCloseTo(12.0, within(0.001));
    }

    @Test
    void liveIsEmptyRatherThanZeroWhenTheDeviceHasNoBuffer() {
        when(liveMetricsStore.getIfPresent(deviceId)).thenReturn(null);

        assertThat(queryService.handle(query(TrendPeriod.LIVE, null, null))).isEmpty();
    }

    @Test
    void withNoStoredSnapshotTheLiveTrendComparesTheWindowToItselfAndReadsFlat() {
        when(liveMetricsStore.getIfPresent(deviceId)).thenReturn(bufferWithCo2(450.0));
        when(snapshotRepository.findLatestByDeviceId(deviceId)).thenReturn(Optional.empty());

        var metrics = queryService.handle(query(TrendPeriod.LIVE, null, null)).orElseThrow();

        assertThat(metrics.co2Trend().deltaPercentage()).isZero();
    }

    @Test
    void theLiveTrendComparesAgainstTheLastStoredSnapshot() {
        when(liveMetricsStore.getIfPresent(deviceId)).thenReturn(bufferWithCo2(500.0));
        when(snapshotRepository.findLatestByDeviceId(deviceId)).thenReturn(Optional.of(snapshot(400.0)));

        var metrics = queryService.handle(query(TrendPeriod.LIVE, null, null)).orElseThrow();

        assertThat(metrics.co2Trend().deltaPercentage()).isCloseTo(25.0, within(0.001));
    }

    @Test
    void aPeriodChoosesTheWindowAndTheTrendComparesTheEqualWindowBeforeIt() {
        when(evaluations.fetchHourlyTelemetryAggregation(any(), any()))
                .thenReturn(java.util.List.of(new com.claircore.evaluation.interfaces.acl.HourlyTelemetryAverage(deviceId, 500.0, 12.0, 23.5, 52.0)))
                .thenReturn(java.util.List.of(new com.claircore.evaluation.interfaces.acl.HourlyTelemetryAverage(deviceId, 400.0, 12.0, 23.5, 52.0)));

        var metrics = queryService.handle(query(TrendPeriod.WEEK, null, null)).orElseThrow();

        assertThat(metrics.averageCo2()).isEqualTo(500.0);
        assertThat(metrics.co2Trend().deltaPercentage()).isCloseTo(25.0, within(0.001));

        var starts = ArgumentCaptor.forClass(Instant.class);
        var ends = ArgumentCaptor.forClass(Instant.class);
        verify(evaluations, times(2))
                .fetchHourlyTelemetryAggregation(starts.capture(), ends.capture());
        // The current window is seven days long, and the comparison window is the seven before it.
        assertThat(Duration.between(starts.getAllValues().get(0), ends.getAllValues().get(0)))
                .isCloseTo(Duration.ofDays(7), Duration.ofSeconds(5));
        assertThat(ends.getAllValues().get(1)).isEqualTo(starts.getAllValues().get(0));
    }

    @Test
    void anExplicitWindowWinsOverThePeriod() {
        Instant start = Instant.parse("2026-05-01T00:00:00Z");
        Instant end = Instant.parse("2026-05-02T00:00:00Z");
        when(evaluations.fetchHourlyTelemetryAggregation(start, end))
                .thenReturn(java.util.List.of(new com.claircore.evaluation.interfaces.acl.HourlyTelemetryAverage(deviceId, 500.0, 12.0, 23.5, 52.0)));
        when(evaluations.fetchHourlyTelemetryAggregation(
                start.minus(1, ChronoUnit.DAYS), start)).thenReturn(java.util.List.of());

        var metrics = queryService.handle(query(TrendPeriod.MONTH, start, end)).orElseThrow();

        assertThat(metrics.averageCo2()).isEqualTo(500.0);
        // No comparison window, so there is no percentage to report rather than a fabricated zero.
        assertThat(metrics.co2Trend().deltaPercentage()).isNull();
    }

    @Test
    void anEmptyHistoricalWindowIsNotFoundRatherThanZeroedMetrics() {
        when(evaluations.fetchHourlyTelemetryAggregation(any(), any()))
                .thenReturn(java.util.List.of());

        // The message is the assertion that matters: it is what reaches the client, and the
        // dedicated exception subclass that used to carry it added nothing else.
        assertThatThrownBy(() -> queryService.handle(query(TrendPeriod.DAY, null, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No telemetry data available for device with ID %s in the requested period."
                        .formatted(deviceId));
    }

    private GetDashboardMetricsQuery query(TrendPeriod period, Instant start, Instant end) {
        return new GetDashboardMetricsQuery(new DeviceId(deviceId), period, start, end);
    }

    private static KpiLiveMetricsBuffer bufferWithCo2(double co2) {
        var buffer = new KpiLiveMetricsBuffer();
        buffer.add(Instant.now(), co2, 12.0, 23.5, 52.0);
        return buffer;
    }

    private static DeviceAnalyticsSnapshot snapshot(double co2) {
        Instant end = Instant.parse("2026-05-16T22:00:00Z");
        return new DeviceAnalyticsSnapshot(
                new DeviceId(UUID.randomUUID()), end.minus(1, ChronoUnit.HOURS), end,
                co2, 12.0, 23.5, 52.0, new AirQualityIndex(55, AqiCategory.MODERATE));
    }
}
