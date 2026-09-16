package com.claircore.evaluation.domain.model.aggregates;

import com.claircore.evaluation.domain.model.valueobjects.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TelemetryEvaluationTest {

    private final DeviceId sampleDeviceId = new DeviceId(UUID.randomUUID());
    private final UUID sampleTime = UUID.fromString("00000000-0000-0000-0000-000000000123");
    private final AirQuality sampleAirQuality = new AirQuality(400.0, 22.0, 45.0);
    private final ParticulateMatter samplePM = new ParticulateMatter(10.0, 15.0, 25.0);
    private final Connectivity sampleConnectivity = new Connectivity("ONLINE", "WiFi", -50);
    private final Location sampleLocation = new Location("Chile");
    private final Instant sampleRecordedAt = Instant.now();

    @Test
    void shouldCreateTelemetryEvaluationWhenValuesAreValid() {
        // Arrange & Act
        TelemetryEvaluation evaluation = new TelemetryEvaluation(
                sampleDeviceId, sampleTime, 3600L, sampleAirQuality, samplePM,
                sampleConnectivity, sampleLocation, 85, "STABLE", sampleRecordedAt
        );

        // Assert
        assertThat(evaluation.getDeviceId()).isEqualTo(sampleDeviceId);
        assertThat(evaluation.getReadingId()).isEqualTo(sampleTime);
        assertThat(evaluation.getUptime()).isEqualTo(3600L);
        assertThat(evaluation.getAirQuality()).isEqualTo(sampleAirQuality);
        assertThat(evaluation.getParticulateMatter()).isEqualTo(samplePM);
        assertThat(evaluation.getConnectivity()).isEqualTo(sampleConnectivity);
        assertThat(evaluation.getLocation()).isEqualTo(sampleLocation);
        assertThat(evaluation.getHealthStatus()).isEqualTo(85);
        assertThat(evaluation.getStatus()).isEqualTo("STABLE");
        assertThat(evaluation.getRecordedAt()).isEqualTo(sampleRecordedAt);
        assertThat(evaluation.getId()).isNotNull();
        assertThat(evaluation.getCreatedAt()).isNull();
    }

    @Test
    void shouldThrowExceptionWhenDeviceIdIsNull() {
        assertThatThrownBy(() -> new TelemetryEvaluation(
                null, sampleTime, 3600L, sampleAirQuality, samplePM,
                sampleConnectivity, sampleLocation, 85, "STABLE", sampleRecordedAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Device ID must not be null");
    }

    @Test
    void shouldThrowExceptionWhenUptimeIsNull() {
        assertThatThrownBy(() -> new TelemetryEvaluation(
                sampleDeviceId, sampleTime, null, sampleAirQuality, samplePM,
                sampleConnectivity, sampleLocation, 85, "STABLE", sampleRecordedAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("uptime must not be null or negative");
    }

    @Test
    void shouldThrowExceptionWhenHealthStatusIsNegative() {
        assertThatThrownBy(() -> new TelemetryEvaluation(
                sampleDeviceId, sampleTime, 3600L, sampleAirQuality, samplePM,
                sampleConnectivity, sampleLocation, -5, "STABLE", sampleRecordedAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("healthStatus must be between 0 and 100");
    }
}
