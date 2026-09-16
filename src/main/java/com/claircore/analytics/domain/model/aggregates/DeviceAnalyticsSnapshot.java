package com.claircore.analytics.domain.model.aggregates;

import com.claircore.analytics.domain.model.valueobjects.AirQualityIndex;
import com.claircore.analytics.domain.model.valueobjects.DeviceId;

import java.time.Instant;
import java.util.UUID;

/** One device's averaged air quality over a closed hourly window, with the AQI derived from it. */
public class DeviceAnalyticsSnapshot {

    private final UUID id;
    private final DeviceId deviceId;
    private final Instant timeWindowStart;
    private final Instant timeWindowEnd;
    private final Double averageCo2;
    private final Double averagePm2_5;
    private final Double averageTemperature;
    private final Double averageHumidity;
    private final AirQualityIndex calculatedAqi;
    private final Instant createdAt;
    private final Instant updatedAt;

    private DeviceAnalyticsSnapshot(
            UUID id,
            DeviceId deviceId,
            Instant timeWindowStart,
            Instant timeWindowEnd,
            Double averageCo2,
            Double averagePm2_5,
            Double averageTemperature,
            Double averageHumidity,
            AirQualityIndex calculatedAqi,
            Instant createdAt,
            Instant updatedAt
    ) {
        if (id == null) {
            throw new IllegalArgumentException("Id must not be null");
        }
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (timeWindowStart == null) {
            throw new IllegalArgumentException("timeWindowStart must not be null");
        }
        if (timeWindowEnd == null) {
            throw new IllegalArgumentException("timeWindowEnd must not be null");
        }
        if (averageCo2 == null) {
            throw new IllegalArgumentException("averageCo2 must not be null");
        }
        if (averagePm2_5 == null) {
            throw new IllegalArgumentException("averagePm2_5 must not be null");
        }
        if (averageTemperature == null) {
            throw new IllegalArgumentException("averageTemperature must not be null");
        }
        if (averageHumidity == null) {
            throw new IllegalArgumentException("averageHumidity must not be null");
        }
        if (calculatedAqi == null) {
            throw new IllegalArgumentException("calculatedAqi must not be null");
        }

        this.id = id;
        this.deviceId = deviceId;
        this.timeWindowStart = timeWindowStart;
        this.timeWindowEnd = timeWindowEnd;
        this.averageCo2 = averageCo2;
        this.averagePm2_5 = averagePm2_5;
        this.averageTemperature = averageTemperature;
        this.averageHumidity = averageHumidity;
        this.calculatedAqi = calculatedAqi;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public DeviceAnalyticsSnapshot(
            DeviceId deviceId,
            Instant timeWindowStart,
            Instant timeWindowEnd,
            Double averageCo2,
            Double averagePm2_5,
            Double averageTemperature,
            Double averageHumidity,
            AirQualityIndex calculatedAqi
    ) {
        this(UUID.randomUUID(), deviceId, timeWindowStart, timeWindowEnd, averageCo2, averagePm2_5,
                averageTemperature, averageHumidity, calculatedAqi, null, null);
    }

    /** Rebuilds a snapshot already in storage; only a persistence assembler should call this. */
    public static DeviceAnalyticsSnapshot reconstitute(
            UUID id,
            DeviceId deviceId,
            Instant timeWindowStart,
            Instant timeWindowEnd,
            Double averageCo2,
            Double averagePm2_5,
            Double averageTemperature,
            Double averageHumidity,
            AirQualityIndex calculatedAqi,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new DeviceAnalyticsSnapshot(id, deviceId, timeWindowStart, timeWindowEnd, averageCo2,
                averagePm2_5, averageTemperature, averageHumidity, calculatedAqi, createdAt, updatedAt);
    }

    public UUID getId() { return id; }
    public DeviceId getDeviceId() { return deviceId; }
    public Instant getTimeWindowStart() { return timeWindowStart; }
    public Instant getTimeWindowEnd() { return timeWindowEnd; }
    public Double getAverageCo2() { return averageCo2; }
    public Double getAveragePm2_5() { return averagePm2_5; }
    public Double getAverageTemperature() { return averageTemperature; }
    public Double getAverageHumidity() { return averageHumidity; }
    public AirQualityIndex getCalculatedAqi() { return calculatedAqi; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
