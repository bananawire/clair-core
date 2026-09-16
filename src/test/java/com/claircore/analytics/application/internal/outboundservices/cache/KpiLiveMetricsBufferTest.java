package com.claircore.analytics.application.internal.outboundservices.cache;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class KpiLiveMetricsBufferTest {

    @Test
    void shouldComputeAveragesCorrectly() {
        var buffer = new KpiLiveMetricsBuffer();
        var now = Instant.now();

        buffer.add(now.minusSeconds(10), 400.0, 10.0, 20.0, 40.0);
        buffer.add(now, 600.0, 20.0, 30.0, 60.0);

        var averages = buffer.computeAverages().orElseThrow();
        assertEquals(500.0, averages.co2());
        assertEquals(15.0, averages.pm2_5());
        assertEquals(25.0, averages.temperature());
        assertEquals(50.0, averages.humidity());
    }

    @Test
    void shouldPruneOldReadingsWhenAddingNewReading() {
        var buffer = new KpiLiveMetricsBuffer();
        var now = Instant.now();

        // 6 minutes ago (should be pruned)
        buffer.add(now.minus(Duration.ofMinutes(6)), 1000.0, 50.0, 25.0, 70.0);
        // 10 seconds ago (should remain)
        buffer.add(now.minusSeconds(10), 400.0, 10.0, 20.0, 40.0);

        var averages = buffer.computeAverages().orElseThrow();
        // Since the 6 minutes ago reading is pruned, the average should only reflect the 10 seconds ago reading
        assertEquals(400.0, averages.co2());
        assertEquals(10.0, averages.pm2_5());
        assertEquals(20.0, averages.temperature());
        assertEquals(40.0, averages.humidity());
    }

    @Test
    void shouldReturnNoDataWhenEmpty() {
        assertTrue(new KpiLiveMetricsBuffer().computeAverages().isEmpty());
    }

    @Test
    void expiresOutOfOrderReadingsAndReportsMeasurementTime() {
        Instant now = Instant.parse("2026-09-07T12:00:00Z");
        var buffer = new KpiLiveMetricsBuffer(java.time.Clock.fixed(now, java.time.ZoneOffset.UTC));
        buffer.add(now.minusSeconds(1), 400, 8, 20, 50);
        buffer.add(now.minusSeconds(600), 900, 500, 20, 50);
        buffer.add(now.minusSeconds(20), 600, 12, 20, 50);
        var window = buffer.computeAverages().orElseThrow();
        assertEquals(2, window.readingCount());
        assertEquals(10, window.pm2_5());
        assertEquals(now.minusSeconds(1), window.measuredAt());
    }
}
