package com.claircore.alerting.domain.model.aggregates;

import com.claircore.alerting.domain.model.valueobjects.AlertSeverity;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.domain.model.valueobjects.MetricType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One threshold breach, from the reading that opened it to the reading that closes it. */
public class Alert {

    private final UUID id;
    private final UUID deviceId;
    private final UUID spaceId;
    private final MetricType metric;
    private final BigDecimal thresholdValue;
    private final BigDecimal actualValue;
    private final String message;
    private AlertStatus status;
    private final AlertSeverity severity;
    private final String spaceName;
    private final String deviceName;
    private final Instant occurredAt;
    private Instant resolvedAt;
    /** Strictly increasing across every alert; bumped on open, acknowledge and resolve. */
    private long transitionSequence;
    /** Highest transition the edge has confirmed it stored; null until the first receipt. */
    private Long edgeReceiptSequence;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Alert(UUID id, UUID deviceId, UUID spaceId, String spaceName, String deviceName,
                  MetricType metric, BigDecimal thresholdValue, BigDecimal actualValue,
                  String message, AlertStatus status, AlertSeverity severity,
                  Instant occurredAt, Instant resolvedAt, long transitionSequence, Long edgeReceiptSequence,
                  Instant createdAt, Instant updatedAt) {
        if (id == null) {
            throw new IllegalArgumentException("Id must not be null");
        }
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (metric == null) {
            throw new IllegalArgumentException("Metric must not be null");
        }
        if (thresholdValue == null || actualValue == null) {
            throw new IllegalArgumentException("Threshold and actual values must not be null");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message must not be null or blank");
        }
        if (severity == null) {
            throw new IllegalArgumentException("Severity must not be null");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("Occurred at must not be null");
        }

        this.id = id;
        this.deviceId = deviceId;
        this.spaceId = spaceId;
        this.spaceName = spaceName;
        this.deviceName = deviceName;
        this.metric = metric;
        this.thresholdValue = thresholdValue;
        this.actualValue = actualValue;
        this.message = message;
        this.status = status;
        this.severity = severity;
        this.occurredAt = occurredAt;
        this.resolvedAt = resolvedAt;
        this.transitionSequence = transitionSequence;
        this.edgeReceiptSequence = edgeReceiptSequence;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Alert(UUID deviceId, UUID spaceId, String spaceName, String deviceName,
                 MetricType metric, BigDecimal thresholdValue, BigDecimal actualValue,
                 String message, AlertSeverity severity, Instant occurredAt) {
        this(UUID.randomUUID(), deviceId, spaceId, spaceName, deviceName, metric, thresholdValue,
                actualValue, message, AlertStatus.ACTIVE, severity, occurredAt, null, 0L, null, null, null);
    }

    /** Rebuilds an alert that already exists in storage, identity and audit timestamps included. */
    public static Alert reconstitute(UUID id, UUID deviceId, UUID spaceId, String spaceName, String deviceName,
                                     MetricType metric, BigDecimal thresholdValue, BigDecimal actualValue,
                                     String message, AlertStatus status, AlertSeverity severity,
                                     Instant occurredAt, Instant resolvedAt, long transitionSequence,
                                     Long edgeReceiptSequence, Instant createdAt, Instant updatedAt) {
        return new Alert(id, deviceId, spaceId, spaceName, deviceName, metric, thresholdValue, actualValue,
                message, status, severity, occurredAt, resolvedAt, transitionSequence, edgeReceiptSequence,
                createdAt, updatedAt);
    }

    public void acknowledge() {
        this.status = AlertStatus.ACKNOWLEDGED;
    }

    public void resolve(Instant resolvedAt) {
        if (resolvedAt == null) {
            throw new IllegalArgumentException("Resolved at must not be null");
        }
        this.status = AlertStatus.RESOLVED;
        this.resolvedAt = resolvedAt;
    }

    /** Stamps the transition that just happened; the sequence comes from the repository's counter. */
    public void markTransition(long sequence) {
        if (sequence <= this.transitionSequence) {
            throw new IllegalArgumentException("Transition sequence must increase");
        }
        this.transitionSequence = sequence;
    }

    /**
     * The edge confirms it stored transition {@code sequence}. Monotonic: an older receipt never
     * lowers the watermark. Returns whether the edge is now current with this alert.
     */
    public boolean recordEdgeReceipt(long sequence) {
        if (edgeReceiptSequence == null || sequence > edgeReceiptSequence) {
            edgeReceiptSequence = Math.min(sequence, transitionSequence);
        }
        return edgeReceiptSequence != null && edgeReceiptSequence >= transitionSequence;
    }

    public long getTransitionSequence() { return transitionSequence; }
    public Long getEdgeReceiptSequence() { return edgeReceiptSequence; }
    public UUID getId() { return id; }
    public UUID getDeviceId() { return deviceId; }
    public UUID getSpaceId() { return spaceId; }
    public MetricType getMetric() { return metric; }
    public BigDecimal getThresholdValue() { return thresholdValue; }
    public BigDecimal getActualValue() { return actualValue; }
    public String getMessage() { return message; }
    public AlertStatus getStatus() { return status; }
    public AlertSeverity getSeverity() { return severity; }
    public String getSpaceName() { return spaceName; }
    public String getDeviceName() { return deviceName; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getResolvedAt() { return resolvedAt; }

    /** Null until the alert has been written; assigned by persistence auditing. */
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
