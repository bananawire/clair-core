package com.claircore.device.domain.model.aggregates;

import com.claircore.device.domain.model.valueobjects.DeviceCommandStatus;
import com.claircore.device.domain.model.valueobjects.DeviceCommandType;

import java.time.Instant;
import java.util.UUID;

/**
 * One instruction queued for a device, from creation to the edge's acknowledgement.
 *
 * <p>Refers to the device by id, not by object: the edge command endpoints used to read
 * {@code getDevice().getHardwareId()} off a lazy association after the transaction closed.
 */
public class DeviceCommand {

    private final UUID id;
    private final UUID deviceId;
    /** The assignment generation this command was issued under; null only on rows older than V4. */
    private final UUID assignmentId;
    private final DeviceCommandType type;
    private DeviceCommandStatus status;
    private final String payload;
    private Instant sentAt;
    private Instant executedAt;
    private String failureReason;
    private final Instant createdAt;
    private final Instant updatedAt;

    private DeviceCommand(UUID id, UUID deviceId, UUID assignmentId, DeviceCommandType type,
                          DeviceCommandStatus status, String payload, Instant sentAt, Instant executedAt,
                          String failureReason, Instant createdAt, Instant updatedAt) {
        if (id == null) {
            throw new IllegalArgumentException("Id must not be null");
        }
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Device command type must not be null");
        }
        this.id = id;
        this.deviceId = deviceId;
        this.assignmentId = assignmentId;
        this.type = type;
        this.status = status;
        this.payload = payload;
        this.sentAt = sentAt;
        this.executedAt = executedAt;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Issues a command under the device's current assignment, so an unlink can void it. */
    public DeviceCommand(UUID deviceId, UUID assignmentId, DeviceCommandType type, String payload) {
        this(UUID.randomUUID(), deviceId, requireAssignmentId(assignmentId), type, DeviceCommandStatus.PENDING,
                payload, null, null, null, null, null);
    }

    /**
     * An unbound command. Only legacy rows written before assignment binding look like this; new
     * production commands go through the bound constructor. Retained for tests and migration reads.
     */
    public DeviceCommand(UUID deviceId, DeviceCommandType type, String payload) {
        this(UUID.randomUUID(), deviceId, null, type, DeviceCommandStatus.PENDING, payload,
                null, null, null, null, null);
    }

    private static UUID requireAssignmentId(UUID assignmentId) {
        if (assignmentId == null) {
            throw new IllegalArgumentException("Assignment ID must not be null");
        }
        return assignmentId;
    }

    /** Rebuilds a command already in storage; only a persistence assembler should call this. */
    public static DeviceCommand reconstitute(
            UUID id, UUID deviceId, UUID assignmentId, DeviceCommandType type, DeviceCommandStatus status,
            String payload, Instant sentAt, Instant executedAt, String failureReason, Instant createdAt,
            Instant updatedAt) {
        return new DeviceCommand(id, deviceId, assignmentId, type, status, payload, sentAt, executedAt,
                failureReason, createdAt, updatedAt);
    }

    /** Whether this command was issued under {@code assignmentId}; unbound legacy rows match any. */
    public boolean belongsToAssignment(UUID assignmentId) {
        return this.assignmentId == null || this.assignmentId.equals(assignmentId);
    }

    /** Voids a command that can no longer be delivered. Terminal states are left as they are. */
    public void expire() {
        if (this.status == DeviceCommandStatus.PENDING || this.status == DeviceCommandStatus.SENT) {
            this.status = DeviceCommandStatus.EXPIRED;
        }
    }

    public void markSent() {
        if (this.status == DeviceCommandStatus.PENDING) {
            this.status = DeviceCommandStatus.SENT;
            this.sentAt = Instant.now();
        }
    }

    /** Renews an expired delivery lease without changing the ACK-required SENT state. */
    public void redeliver() {
        if (this.status == DeviceCommandStatus.SENT) this.sentAt = Instant.now();
    }

    public void markExecuted() {
        this.status = DeviceCommandStatus.EXECUTED;
        this.executedAt = Instant.now();
        this.failureReason = null;
    }

    public void markFailed(String failureReason) {
        this.status = DeviceCommandStatus.FAILED;
        this.executedAt = Instant.now();
        this.failureReason = failureReason;
    }

    public UUID getId() { return id; }
    public UUID getDeviceId() { return deviceId; }
    public UUID getAssignmentId() { return assignmentId; }
    public DeviceCommandType getType() { return type; }
    public DeviceCommandStatus getStatus() { return status; }
    public String getPayload() { return payload; }
    public Instant getSentAt() { return sentAt; }
    public Instant getExecutedAt() { return executedAt; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
