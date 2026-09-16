package com.claircore.evaluation.domain.model.aggregates;

import com.claircore.evaluation.domain.model.valueobjects.AirQuality;
import com.claircore.evaluation.domain.model.valueobjects.Connectivity;
import com.claircore.evaluation.domain.model.valueobjects.DeviceId;
import com.claircore.evaluation.domain.model.valueobjects.Location;
import com.claircore.evaluation.domain.model.valueobjects.ParticulateMatter;

import java.time.Instant;
import java.util.UUID;

/**
 * One telemetry reading, as evaluated and stored. Immutable: a reading is a fact about a moment,
 * never revised.
 */
public class TelemetryEvaluation {

    private final UUID id;
    private final DeviceId deviceId;
    private final AirQuality airQuality;
    private final ParticulateMatter particulateMatter;
    private final Connectivity connectivity;
    private final Location location;
    private final UUID readingId;
    private final Long uptime;
    private final String status;
    private final Integer healthStatus;
    private final Instant recordedAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    private TelemetryEvaluation(
            UUID id,
            DeviceId deviceId,
            UUID readingId,
            Long uptime,
            AirQuality airQuality,
            ParticulateMatter particulateMatter,
            Connectivity connectivity,
            Location location,
            Integer healthStatus,
            String status,
            Instant recordedAt,
            Instant createdAt,
            Instant updatedAt) {
        if (id == null) {
            throw new IllegalArgumentException("Id must not be null");
        }
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (readingId == null) {
            throw new IllegalArgumentException("readingId must not be null");
        }
        if (uptime == null || uptime < 0) {
            throw new IllegalArgumentException("uptime must not be null or negative");
        }
        if (airQuality == null) {
            throw new IllegalArgumentException("airQuality must not be null");
        }
        if (particulateMatter == null) {
            throw new IllegalArgumentException("particulateMatter must not be null");
        }
        if (connectivity == null) {
            throw new IllegalArgumentException("connectivity must not be null");
        }
        if (location == null) {
            throw new IllegalArgumentException("location must not be null");
        }
        if (healthStatus == null || healthStatus < 0 || healthStatus > 100) {
            throw new IllegalArgumentException("healthStatus must be between 0 and 100");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status must not be null or blank");
        }
        if (recordedAt == null) {
            throw new IllegalArgumentException("recordedAt must not be null");
        }

        this.id = id;
        this.deviceId = deviceId;
        this.readingId = readingId;
        this.uptime = uptime;
        this.airQuality = airQuality;
        this.particulateMatter = particulateMatter;
        this.connectivity = connectivity;
        this.location = location;
        this.healthStatus = healthStatus;
        this.status = status;
        this.recordedAt = recordedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public TelemetryEvaluation(
            DeviceId deviceId,
            UUID readingId,
            Long uptime,
            AirQuality airQuality,
            ParticulateMatter particulateMatter,
            Connectivity connectivity,
            Location location,
            Integer healthStatus,
            String status,
            Instant recordedAt
    ) {
        this(UUID.randomUUID(), deviceId, readingId, uptime, airQuality, particulateMatter, connectivity,
                location, healthStatus, status, recordedAt, null, null);
    }

    /** Rebuilds a reading that already exists in storage, identity and audit timestamps included. */
    public static TelemetryEvaluation reconstitute(
            UUID id,
            DeviceId deviceId,
            UUID readingId,
            Long uptime,
            AirQuality airQuality,
            ParticulateMatter particulateMatter,
            Connectivity connectivity,
            Location location,
            Integer healthStatus,
            String status,
            Instant recordedAt,
            Instant createdAt,
            Instant updatedAt) {
        return new TelemetryEvaluation(id, deviceId, readingId, uptime, airQuality, particulateMatter,
                connectivity, location, healthStatus, status, recordedAt, createdAt, updatedAt);
    }

    public UUID getId() { return id; }
    public DeviceId getDeviceId() { return deviceId; }
    public AirQuality getAirQuality() { return airQuality; }
    public ParticulateMatter getParticulateMatter() { return particulateMatter; }
    public Connectivity getConnectivity() { return connectivity; }
    public Location getLocation() { return location; }
    public UUID getReadingId() { return readingId; }
    public Long getUptime() { return uptime; }
    public Integer getHealthStatus() { return healthStatus; }
    public String getStatus() { return status; }
    public Instant getRecordedAt() { return recordedAt; }

    /** Null until the reading has been written; assigned by persistence auditing. */
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
