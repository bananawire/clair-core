package com.claircore.analytics.domain.model.aggregates;

import com.claircore.analytics.domain.model.valueobjects.AqiCategory;
import com.claircore.analytics.domain.model.valueobjects.AqiCategoryBreakdown;
import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.domain.model.valueobjects.MetricStats;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Pre-computed summary of one device's air quality over one calendar day
 * (America/Lima). Built nightly from raw telemetry, so min/max/peak are true
 * extremes. One row per device per local date.
 */
public class DeviceDailySummary {

    private final UUID id;
    private final DeviceId deviceId;
    private final LocalDate summaryDate;
    private final MetricStats co2;
    private final MetricStats pm2_5;
    private final MetricStats temperature;
    private final MetricStats humidity;
    private final Double peakPm2_5;
    private final Instant peakPm2_5At;
    private final Integer averageAqi;
    private final AqiCategory dominantAqiCategory;
    private final AqiCategoryBreakdown categoryBreakdown;
    private final long readingCount;
    /** AQI change vs the previous day, as a percentage. Null when no prior day exists. */
    private final Double aqiDeltaPct;
    private final Instant createdAt;
    private final Instant updatedAt;

    private DeviceDailySummary(
            UUID id,
            DeviceId deviceId,
            LocalDate summaryDate,
            MetricStats co2,
            MetricStats pm2_5,
            MetricStats temperature,
            MetricStats humidity,
            Double peakPm2_5,
            Instant peakPm2_5At,
            Integer averageAqi,
            AqiCategory dominantAqiCategory,
            AqiCategoryBreakdown categoryBreakdown,
            long readingCount,
            Double aqiDeltaPct,
            Instant createdAt,
            Instant updatedAt
    ) {
        if (id == null) throw new IllegalArgumentException("Id must not be null");
        if (deviceId == null) throw new IllegalArgumentException("deviceId must not be null");
        if (summaryDate == null) throw new IllegalArgumentException("summaryDate must not be null");
        if (readingCount <= 0) throw new IllegalArgumentException("readingCount must be positive");
        this.id = id;
        this.deviceId = deviceId;
        this.summaryDate = summaryDate;
        this.co2 = co2;
        this.pm2_5 = pm2_5;
        this.temperature = temperature;
        this.humidity = humidity;
        this.peakPm2_5 = peakPm2_5;
        this.peakPm2_5At = peakPm2_5At;
        this.averageAqi = averageAqi;
        this.dominantAqiCategory = dominantAqiCategory;
        this.categoryBreakdown = categoryBreakdown;
        this.readingCount = readingCount;
        this.aqiDeltaPct = aqiDeltaPct;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public DeviceDailySummary(
            DeviceId deviceId,
            LocalDate summaryDate,
            MetricStats co2,
            MetricStats pm2_5,
            MetricStats temperature,
            MetricStats humidity,
            Double peakPm2_5,
            Instant peakPm2_5At,
            Integer averageAqi,
            AqiCategory dominantAqiCategory,
            AqiCategoryBreakdown categoryBreakdown,
            long readingCount,
            Double aqiDeltaPct
    ) {
        this(UUID.randomUUID(), deviceId, summaryDate, co2, pm2_5, temperature, humidity, peakPm2_5,
                peakPm2_5At, averageAqi, dominantAqiCategory, categoryBreakdown, readingCount,
                aqiDeltaPct, null, null);
    }

    /** Rebuilds a summary already in storage; only a persistence assembler should call this. */
    public static DeviceDailySummary reconstitute(
            UUID id,
            DeviceId deviceId,
            LocalDate summaryDate,
            MetricStats co2,
            MetricStats pm2_5,
            MetricStats temperature,
            MetricStats humidity,
            Double peakPm2_5,
            Instant peakPm2_5At,
            Integer averageAqi,
            AqiCategory dominantAqiCategory,
            AqiCategoryBreakdown categoryBreakdown,
            long readingCount,
            Double aqiDeltaPct,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new DeviceDailySummary(id, deviceId, summaryDate, co2, pm2_5, temperature, humidity,
                peakPm2_5, peakPm2_5At, averageAqi, dominantAqiCategory, categoryBreakdown,
                readingCount, aqiDeltaPct, createdAt, updatedAt);
    }

    public DeviceDailySummary withPreviousAqi(Integer previous) {
        Double delta = previous == null || previous <= 0 ? null : (averageAqi - previous) * 100.0 / previous;
        return new DeviceDailySummary(id, deviceId, summaryDate, co2, pm2_5, temperature, humidity,
                peakPm2_5, peakPm2_5At, averageAqi, dominantAqiCategory, categoryBreakdown,
                readingCount, delta, createdAt, updatedAt);
    }

    public UUID getId() { return id; }
    public DeviceId getDeviceId() { return deviceId; }
    public LocalDate getSummaryDate() { return summaryDate; }
    public MetricStats getCo2() { return co2; }
    public MetricStats getPm2_5() { return pm2_5; }
    public MetricStats getTemperature() { return temperature; }
    public MetricStats getHumidity() { return humidity; }
    public Double getPeakPm2_5() { return peakPm2_5; }
    public Instant getPeakPm2_5At() { return peakPm2_5At; }
    public Integer getAverageAqi() { return averageAqi; }
    public AqiCategory getDominantAqiCategory() { return dominantAqiCategory; }
    public AqiCategoryBreakdown getCategoryBreakdown() { return categoryBreakdown; }
    public long getReadingCount() { return readingCount; }
    public Double getAqiDeltaPct() { return aqiDeltaPct; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
