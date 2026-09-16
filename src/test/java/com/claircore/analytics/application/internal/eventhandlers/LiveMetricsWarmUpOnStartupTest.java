package com.claircore.analytics.application.internal.eventhandlers;

import com.claircore.analytics.application.internal.outboundservices.acl.ExternalEvaluationService;
import com.claircore.analytics.application.internal.outboundservices.cache.KpiLiveMetricsBuffer;
import com.claircore.analytics.application.internal.outboundservices.cache.LiveMetricsStore;
import com.claircore.evaluation.interfaces.acl.TelemetryReading;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LiveMetricsWarmUpOnStartupTest {
    @Test
    void refillsTheLiveWindowFromRecentReadingsAfterARestart() {
        UUID device = UUID.randomUUID();
        Instant now = Instant.now();
        var evaluations = mock(ExternalEvaluationService.class);
        when(evaluations.fetchReadings(any(), any())).thenReturn(List.of(
                new TelemetryReading(device, 500, 12, 22, 40, now.minusSeconds(30)),
                new TelemetryReading(device, 700, 14, 23, 41, now.minusSeconds(10))));
        var store = new LiveMetricsStore() {
            final KpiLiveMetricsBuffer buffer = new KpiLiveMetricsBuffer();
            public KpiLiveMetricsBuffer getOrCreate(UUID deviceId) { return buffer; }
            public KpiLiveMetricsBuffer getIfPresent(UUID deviceId) { return buffer; }
        };
        int warmed = new LiveMetricsWarmUpOnStartup(evaluations, store).warmUp(now);
        assertThat(warmed).isEqualTo(2);
        var averages = store.buffer.computeAverages().orElseThrow();
        assertThat(averages.readingCount()).isEqualTo(2);
        assertThat(averages.co2()).isEqualTo(600.0);
    }

    @Test
    void aFailingReadDoesNotStopStartup() {
        var evaluations = mock(ExternalEvaluationService.class);
        when(evaluations.fetchReadings(any(), any())).thenThrow(new IllegalStateException("db down"));
        var store = mock(LiveMetricsStore.class);
        assertThat(new LiveMetricsWarmUpOnStartup(evaluations, store).warmUp(Instant.now())).isZero();
    }
}
