package com.claircore.alerting.infrastructure.persistence.jpa.entities;

import com.claircore.alerting.domain.model.valueobjects.AlertSeverity;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.domain.model.valueobjects.MetricType;
import com.claircore.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Storage shape of {@code Alert}. */
@Entity
@Table(
        name = "alerts",
        indexes = {
                @Index(name = "idx_alert_device_metric_status", columnList = "deviceId, metric, status"),
                @Index(name = "idx_alert_space_status", columnList = "spaceId, status"),
                @Index(name = "idx_alert_occurred_at", columnList = "occurredAt"),
                @Index(name = "idx_alert_transition_sequence", columnList = "transitionSequence")
        }
)
public class AlertPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(nullable = false)
    private UUID deviceId;

    @Column(name = "space_id")
    private UUID spaceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetricType metric;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal thresholdValue;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal actualValue;

    @Column(nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity severity;

    @Column(name = "space_name")
    private String spaceName;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
    @Column(name = "transition_sequence", nullable = false)
    private long transitionSequence;
    @Column(name = "edge_receipt_sequence")
    private Long edgeReceiptSequence;

    public AlertPersistenceEntity() {
        // JPA, and the persistence assembler
    }

    public UUID getDeviceId() { return deviceId; }
    public void setDeviceId(UUID deviceId) { this.deviceId = deviceId; }

    public UUID getSpaceId() { return spaceId; }
    public void setSpaceId(UUID spaceId) { this.spaceId = spaceId; }

    public MetricType getMetric() { return metric; }
    public void setMetric(MetricType metric) { this.metric = metric; }

    public BigDecimal getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(BigDecimal thresholdValue) { this.thresholdValue = thresholdValue; }

    public BigDecimal getActualValue() { return actualValue; }
    public void setActualValue(BigDecimal actualValue) { this.actualValue = actualValue; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public AlertStatus getStatus() { return status; }
    public void setStatus(AlertStatus status) { this.status = status; }

    public AlertSeverity getSeverity() { return severity; }
    public void setSeverity(AlertSeverity severity) { this.severity = severity; }

    public String getSpaceName() { return spaceName; }
    public void setSpaceName(String spaceName) { this.spaceName = spaceName; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }

    public Instant getResolvedAt() { return resolvedAt; }
    public long getTransitionSequence() { return transitionSequence; }
    public void setTransitionSequence(long transitionSequence) { this.transitionSequence = transitionSequence; }
    public Long getEdgeReceiptSequence() { return edgeReceiptSequence; }
    public void setEdgeReceiptSequence(Long edgeReceiptSequence) { this.edgeReceiptSequence = edgeReceiptSequence; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}
