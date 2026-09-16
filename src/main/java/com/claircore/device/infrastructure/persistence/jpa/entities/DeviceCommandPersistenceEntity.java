package com.claircore.device.infrastructure.persistence.jpa.entities;

import com.claircore.device.domain.model.valueobjects.DeviceCommandStatus;
import com.claircore.device.domain.model.valueobjects.DeviceCommandType;
import com.claircore.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Storage shape of {@code DeviceCommand}; {@code device_id} keeps the join column's name. */
@Entity
@Table(name = "device_commands", indexes = @jakarta.persistence.Index(name = "idx_device_commands_assignment", columnList = "assignment_id"))
public class DeviceCommandPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;
    @Column(name = "assignment_id")
    private UUID assignmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceCommandType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceCommandStatus status;

    @Column(columnDefinition = "text")
    private String payload;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    public DeviceCommandPersistenceEntity() {
        // JPA, and the persistence assembler
    }

    public UUID getDeviceId() { return deviceId; }
    public UUID getAssignmentId() { return assignmentId; }
    public void setAssignmentId(UUID assignmentId) { this.assignmentId = assignmentId; }
    public void setDeviceId(UUID deviceId) { this.deviceId = deviceId; }

    public DeviceCommandType getType() { return type; }
    public void setType(DeviceCommandType type) { this.type = type; }

    public DeviceCommandStatus getStatus() { return status; }
    public void setStatus(DeviceCommandStatus status) { this.status = status; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }

    public Instant getExecutedAt() { return executedAt; }
    public void setExecutedAt(Instant executedAt) { this.executedAt = executedAt; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
