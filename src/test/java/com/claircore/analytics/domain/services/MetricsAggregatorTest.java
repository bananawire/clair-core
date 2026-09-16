package com.claircore.analytics.domain.services;

import com.claircore.analytics.domain.model.valueobjects.AirQualityIndex;
import com.claircore.analytics.domain.model.valueobjects.AqiCategory;
import com.claircore.analytics.domain.model.valueobjects.DeviceMetricsSnapshot;
import com.claircore.analytics.domain.model.valueobjects.Freshness;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricsAggregatorTest {

    @Mock
    private AqiCalculator aqiCalculator;

    @InjectMocks
    private MetricsAggregator metricsAggregationService;

    @Test
    void shouldReturnEmptyMetricsWhenSnapshotListIsEmpty() {
        var result = metricsAggregationService.aggregate(Collections.emptyList());
        assertNotNull(result);
        assertNull(result.aqiValue());
        assertNull(result.aqiCategory());
        assertEquals(Freshness.NO_DATA, result.freshness());
    }

    @Test
    void shouldReturnEmptyMetricsWhenSnapshotListIsNull() {
        var result = metricsAggregationService.aggregate(null);
        assertNotNull(result);
        assertNull(result.aqiValue());
        assertEquals(Freshness.NO_DATA, result.freshness());
    }

    @Test
    void shouldAggregateMetricsCorrectly() {
        var now = Instant.now();
        var snapshot1 = new DeviceMetricsSnapshot(
                DeviceMetricsSnapshot.Source.LIVE,
                80, 400.0, 12.0, 22.0, 50.0,
                1.0, 2.0, 0.5, -1.0,
                now.minusSeconds(10)
        );
        var snapshot2 = new DeviceMetricsSnapshot(
                DeviceMetricsSnapshot.Source.SNAPSHOT,
                90, 500.0, 14.0, 24.0, 60.0,
                2.0, 4.0, 1.5, -3.0,
                now
        );

        when(aqiCalculator.calculateAqi(13.0))
                .thenReturn(new AirQualityIndex(85, AqiCategory.MODERATE));

        var result = metricsAggregationService.aggregate(List.of(snapshot1, snapshot2));

        assertNotNull(result);
        // Averages: CO2: 450.0, PM2.5: 13.0, Temp: 23.0, Hum: 55.0
        // Deltas: CO2: 1.5, PM2.5: 3.0, Temp: 1.0, Hum: -2.0
        assertEquals(85, result.aqiValue());
        assertEquals("MODERATE", result.aqiCategory());
        assertEquals(450.0, result.averageCo2());
        assertEquals(13.0, result.averagePm2_5());
        assertEquals(23.0, result.averageTemperature());
        assertEquals(55.0, result.averageHumidity());
        assertEquals(1.5, result.co2DeltaPercentage());
        assertEquals(3.0, result.pm2_5DeltaPercentage());
        assertEquals(1.0, result.temperatureDeltaPercentage());
        assertEquals(-2.0, result.humidityDeltaPercentage());
        assertEquals(now, result.recordedAt());
        assertEquals(Freshness.LIVE, result.freshness());

        verify(aqiCalculator).calculateAqi(13.0);
    }
    @Test
    void co2AndOldIndexCannotPoisonCleanPmOrFabricateMissingPm() {
        var clean = new com.claircore.analytics.domain.model.valueobjects.DeviceMetricsSnapshot(
                com.claircore.analytics.domain.model.valueobjects.DeviceMetricsSnapshot.Source.LIVE,
                500, 400.5, 8.0, 22.0, 50.0, null, null, null, null, java.time.Instant.now());
        var metrics = new MetricsAggregator(new AqiCalculator()).aggregate(java.util.List.of(clean));
        assertEquals(44, metrics.aqiValue());
        assertEquals("GOOD", metrics.aqiCategory());
        var missing = new com.claircore.analytics.domain.model.valueobjects.DeviceMetricsSnapshot(
                com.claircore.analytics.domain.model.valueobjects.DeviceMetricsSnapshot.Source.LIVE,
                500, 400.5, null, 22.0, 50.0, null, null, null, null, java.time.Instant.now());
        assertNull(new MetricsAggregator(new AqiCalculator()).aggregate(java.util.List.of(missing)).aqiValue());
    }
}
