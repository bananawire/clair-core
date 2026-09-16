package com.claircore.analytics.infrastructure.persistence.jpa.entities;

import com.claircore.analytics.domain.model.valueobjects.AqiCategory;
import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.infrastructure.persistence.jpa.embeddables.AqiCategoryBreakdownPersistenceEmbeddable;
import com.claircore.analytics.infrastructure.persistence.jpa.embeddables.MetricStatsPersistenceEmbeddable;
import com.claircore.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDate;

/** Storage shape of {@code DeviceDailySummary}. */
@Entity
@Table(name = "device_daily_summaries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"device_id", "summary_date"}))
public class DeviceDailySummaryPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "device_id", nullable = false)
    private DeviceId deviceId;

    @Column(name = "summary_date", nullable = false)
    private LocalDate summaryDate;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "avg", column = @Column(name = "co2_avg", nullable = false)),
            @AttributeOverride(name = "min", column = @Column(name = "co2_min", nullable = false)),
            @AttributeOverride(name = "max", column = @Column(name = "co2_max", nullable = false))
    })
    private MetricStatsPersistenceEmbeddable co2;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "avg", column = @Column(name = "pm2_5_avg", nullable = false)),
            @AttributeOverride(name = "min", column = @Column(name = "pm2_5_min", nullable = false)),
            @AttributeOverride(name = "max", column = @Column(name = "pm2_5_max", nullable = false))
    })
    private MetricStatsPersistenceEmbeddable pm2_5;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "avg", column = @Column(name = "temperature_avg", nullable = false)),
            @AttributeOverride(name = "min", column = @Column(name = "temperature_min", nullable = false)),
            @AttributeOverride(name = "max", column = @Column(name = "temperature_max", nullable = false))
    })
    private MetricStatsPersistenceEmbeddable temperature;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "avg", column = @Column(name = "humidity_avg", nullable = false)),
            @AttributeOverride(name = "min", column = @Column(name = "humidity_min", nullable = false)),
            @AttributeOverride(name = "max", column = @Column(name = "humidity_max", nullable = false))
    })
    private MetricStatsPersistenceEmbeddable humidity;

    @Column(name = "peak_pm2_5", nullable = false)
    private Double peakPm2_5;

    @Column(name = "peak_pm2_5_at", nullable = false)
    private Instant peakPm2_5At;

    @Column(name = "average_aqi", nullable = false)
    private Integer averageAqi;

    @Enumerated(EnumType.STRING)
    @Column(name = "dominant_aqi_category", nullable = false)
    private AqiCategory dominantAqiCategory;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "good", column = @Column(name = "cat_good", nullable = false)),
            @AttributeOverride(name = "moderate", column = @Column(name = "cat_moderate", nullable = false)),
            @AttributeOverride(name = "unhealthyForSensitive", column = @Column(name = "cat_unhealthy_sensitive", nullable = false)),
            @AttributeOverride(name = "unhealthy", column = @Column(name = "cat_unhealthy", nullable = false)),
            @AttributeOverride(name = "veryUnhealthy", column = @Column(name = "cat_very_unhealthy", nullable = false)),
            @AttributeOverride(name = "hazardous", column = @Column(name = "cat_hazardous", nullable = false))
    })
    private AqiCategoryBreakdownPersistenceEmbeddable categoryBreakdown;

    @Column(name = "reading_count", nullable = false)
    private long readingCount;

    @Column(name = "aqi_delta_pct")
    private Double aqiDeltaPct;

    public DeviceDailySummaryPersistenceEntity() {
        // JPA, and the persistence assembler
    }

    public DeviceId getDeviceId() { return deviceId; }
    public void setDeviceId(DeviceId deviceId) { this.deviceId = deviceId; }

    public MetricStatsPersistenceEmbeddable getCo2() { return co2; }
    public void setCo2(MetricStatsPersistenceEmbeddable co2) { this.co2 = co2; }

    public MetricStatsPersistenceEmbeddable getPm2_5() { return pm2_5; }
    public void setPm2_5(MetricStatsPersistenceEmbeddable pm2_5) { this.pm2_5 = pm2_5; }

    public MetricStatsPersistenceEmbeddable getTemperature() { return temperature; }
    public void setTemperature(MetricStatsPersistenceEmbeddable temperature) { this.temperature = temperature; }

    public MetricStatsPersistenceEmbeddable getHumidity() { return humidity; }
    public void setHumidity(MetricStatsPersistenceEmbeddable humidity) { this.humidity = humidity; }

    public Double getPeakPm2_5() { return peakPm2_5; }
    public void setPeakPm2_5(Double peakPm2_5) { this.peakPm2_5 = peakPm2_5; }

    public Instant getPeakPm2_5At() { return peakPm2_5At; }
    public void setPeakPm2_5At(Instant peakPm2_5At) { this.peakPm2_5At = peakPm2_5At; }

    public Integer getAverageAqi() { return averageAqi; }
    public void setAverageAqi(Integer averageAqi) { this.averageAqi = averageAqi; }

    public AqiCategory getDominantAqiCategory() { return dominantAqiCategory; }
    public void setDominantAqiCategory(AqiCategory dominantAqiCategory) { this.dominantAqiCategory = dominantAqiCategory; }

    public AqiCategoryBreakdownPersistenceEmbeddable getCategoryBreakdown() { return categoryBreakdown; }
    public void setCategoryBreakdown(AqiCategoryBreakdownPersistenceEmbeddable categoryBreakdown) { this.categoryBreakdown = categoryBreakdown; }

    public long getReadingCount() { return readingCount; }
    public void setReadingCount(long readingCount) { this.readingCount = readingCount; }

    public Double getAqiDeltaPct() { return aqiDeltaPct; }
    public void setAqiDeltaPct(Double aqiDeltaPct) { this.aqiDeltaPct = aqiDeltaPct; }

    public LocalDate getSummaryDate() { return summaryDate; }
    public void setSummaryDate(LocalDate summaryDate) { this.summaryDate = summaryDate; }
}
