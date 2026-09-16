package com.claircore.evaluation.infrastructure.persistence.jpa.entities;

import com.claircore.evaluation.domain.model.valueobjects.DeviceId;
import com.claircore.evaluation.infrastructure.persistence.jpa.embeddables.AirQualityPersistenceEmbeddable;
import com.claircore.evaluation.infrastructure.persistence.jpa.embeddables.ConnectivityPersistenceEmbeddable;
import com.claircore.evaluation.infrastructure.persistence.jpa.embeddables.LocationPersistenceEmbeddable;
import com.claircore.evaluation.infrastructure.persistence.jpa.embeddables.ParticulateMatterPersistenceEmbeddable;
import com.claircore.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Storage shape of {@code TelemetryEvaluation}.
 *
 * <p>Every {@code @AttributeOverride} is carried over verbatim from the aggregate: analytics reads
 * this table with raw SQL by column name, so a renamed column is a broken report, not a failed
 * test.
 */
@Entity
@Table(
        name = "telemetry_evaluations",
        uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = {"device_id", "reading_id"}),
        indexes = {
                @Index(name = "idx_telemetry_eval_device_recorded", columnList = "device_id, recorded_at")
        }
)
public class TelemetryEvaluationPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "device_id", nullable = false)
    private DeviceId deviceId;

    @Embedded
    @AttributeOverride(name = "co2", column = @Column(name = "aq_co2", nullable = false))
    @AttributeOverride(name = "temperature", column = @Column(name = "aq_temperature", nullable = false))
    @AttributeOverride(name = "humidity", column = @Column(name = "aq_humidity", nullable = false))
    private AirQualityPersistenceEmbeddable airQuality;

    @Embedded
    @AttributeOverride(name = "pm1_0", column = @Column(name = "pm_pm1_0", nullable = false))
    @AttributeOverride(name = "pm2_5", column = @Column(name = "pm_pm2_5", nullable = false))
    @AttributeOverride(name = "pm10", column = @Column(name = "pm_pm10", nullable = false))
    private ParticulateMatterPersistenceEmbeddable particulateMatter;

    @Embedded
    @AttributeOverride(name = "status", column = @Column(name = "conn_status", nullable = false))
    @AttributeOverride(name = "network", column = @Column(name = "conn_network"))
    @AttributeOverride(name = "signalStrength", column = @Column(name = "conn_signal_strength"))
    private ConnectivityPersistenceEmbeddable connectivity;

    @Embedded
    @AttributeOverride(name = "country", column = @Column(name = "location_country"))
    private LocationPersistenceEmbeddable location;

    @Column(name = "reading_id", nullable = false)
    private UUID readingId;

    @Column(name = "uptime_seconds", nullable = false)
    private Long uptime;

    @Column(nullable = false)
    private String status;

    @Column(name = "health_status", nullable = false)
    private Integer healthStatus;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;
    /** When alerting finished with this reading; null means the after-commit handler never ran. */
    @Column(name = "alerts_evaluated_at")
    private Instant alertsEvaluatedAt;

    public TelemetryEvaluationPersistenceEntity() {
        // JPA, and the persistence assembler
    }

    public DeviceId getDeviceId() { return deviceId; }
    public void setDeviceId(DeviceId deviceId) { this.deviceId = deviceId; }

    public AirQualityPersistenceEmbeddable getAirQuality() { return airQuality; }
    public void setAirQuality(AirQualityPersistenceEmbeddable airQuality) { this.airQuality = airQuality; }

    public ParticulateMatterPersistenceEmbeddable getParticulateMatter() { return particulateMatter; }
    public void setParticulateMatter(ParticulateMatterPersistenceEmbeddable particulateMatter) { this.particulateMatter = particulateMatter; }

    public ConnectivityPersistenceEmbeddable getConnectivity() { return connectivity; }
    public void setConnectivity(ConnectivityPersistenceEmbeddable connectivity) { this.connectivity = connectivity; }

    public LocationPersistenceEmbeddable getLocation() { return location; }
    public void setLocation(LocationPersistenceEmbeddable location) { this.location = location; }

    public UUID getReadingId() { return readingId; }
    public void setReadingId(UUID readingId) { this.readingId = readingId; }

    public Long getUptime() { return uptime; }
    public void setUptime(Long uptime) { this.uptime = uptime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getHealthStatus() { return healthStatus; }
    public void setHealthStatus(Integer healthStatus) { this.healthStatus = healthStatus; }

    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }
    public Instant getAlertsEvaluatedAt() { return alertsEvaluatedAt; }
    public void setAlertsEvaluatedAt(Instant alertsEvaluatedAt) { this.alertsEvaluatedAt = alertsEvaluatedAt; }
}
