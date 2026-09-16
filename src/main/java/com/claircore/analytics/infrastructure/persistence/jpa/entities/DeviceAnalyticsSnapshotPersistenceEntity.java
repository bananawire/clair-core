package com.claircore.analytics.infrastructure.persistence.jpa.entities;

import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.infrastructure.persistence.jpa.embeddables.AirQualityIndexPersistenceEmbeddable;
import com.claircore.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

/** Storage shape of {@code DeviceAnalyticsSnapshot}. */
@Entity
@Table(name = "device_analytics_snapshots")
public class DeviceAnalyticsSnapshotPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "device_id", nullable = false)
    private DeviceId deviceId;

    @Column(name = "time_window_start", nullable = false)
    private Instant timeWindowStart;

    @Column(name = "time_window_end", nullable = false)
    private Instant timeWindowEnd;

    @Column(name = "average_co2", nullable = false)
    private Double averageCo2;

    @Column(name = "average_pm2_5", nullable = false)
    private Double averagePm2_5;

    @Column(name = "average_temperature", nullable = false)
    private Double averageTemperature;

    @Column(name = "average_humidity", nullable = false)
    private Double averageHumidity;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "aqi_value", nullable = false))
    @AttributeOverride(name = "category", column = @Column(name = "aqi_category", nullable = false))
    private AirQualityIndexPersistenceEmbeddable calculatedAqi;

    public DeviceAnalyticsSnapshotPersistenceEntity() {
        // JPA, and the persistence assembler
    }

    public DeviceId getDeviceId() { return deviceId; }
    public void setDeviceId(DeviceId deviceId) { this.deviceId = deviceId; }

    public Instant getTimeWindowStart() { return timeWindowStart; }
    public void setTimeWindowStart(Instant timeWindowStart) { this.timeWindowStart = timeWindowStart; }

    public Instant getTimeWindowEnd() { return timeWindowEnd; }
    public void setTimeWindowEnd(Instant timeWindowEnd) { this.timeWindowEnd = timeWindowEnd; }

    public Double getAverageCo2() { return averageCo2; }
    public void setAverageCo2(Double averageCo2) { this.averageCo2 = averageCo2; }

    public Double getAveragePm2_5() { return averagePm2_5; }
    public void setAveragePm2_5(Double averagePm2_5) { this.averagePm2_5 = averagePm2_5; }

    public Double getAverageTemperature() { return averageTemperature; }
    public void setAverageTemperature(Double averageTemperature) { this.averageTemperature = averageTemperature; }

    public Double getAverageHumidity() { return averageHumidity; }
    public void setAverageHumidity(Double averageHumidity) { this.averageHumidity = averageHumidity; }

    public AirQualityIndexPersistenceEmbeddable getCalculatedAqi() { return calculatedAqi; }
    public void setCalculatedAqi(AirQualityIndexPersistenceEmbeddable calculatedAqi) { this.calculatedAqi = calculatedAqi; }
}
