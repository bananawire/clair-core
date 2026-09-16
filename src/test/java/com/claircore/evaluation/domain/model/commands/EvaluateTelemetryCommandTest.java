package com.claircore.evaluation.domain.model.commands;

import com.claircore.evaluation.domain.model.valueobjects.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvaluateTelemetryCommandTest {

    private final DeviceId sampleDeviceId = new DeviceId(UUID.randomUUID());
    private final UUID sampleTime = UUID.fromString("00000000-0000-0000-0000-000000000123");
    private final AirQuality sampleAirQuality = new AirQuality(400.0, 22.0, 45.0);
    private final ParticulateMatter samplePM = new ParticulateMatter(10.0, 15.0, 25.0);
    private final Connectivity sampleConnectivity = new Connectivity("ONLINE", "WiFi", -50);
    private final Location sampleLocation = new Location("Chile");

    @Test
    void shouldCreateCommandWhenDataIsValid() {
        // Arrange & Act
        EvaluateTelemetryCommand command = new EvaluateTelemetryCommand(
                sampleDeviceId, sampleTime, 3600L, sampleAirQuality, samplePM,
                sampleConnectivity, sampleLocation, 85, "STABLE", Instant.now()
        );

        // Assert
        assertThat(command.deviceId()).isEqualTo(sampleDeviceId);
        assertThat(command.uptime()).isEqualTo(3600L);
        assertThat(command.healthStatus()).isEqualTo(85);
        assertThat(command.status()).isEqualTo("STABLE");
    }

    @Test
    void shouldThrowExceptionWhenDeviceIdIsNull() {
        assertThatThrownBy(() -> new EvaluateTelemetryCommand(
                null, sampleTime, 3600L, sampleAirQuality, samplePM,
                sampleConnectivity, sampleLocation, 85, "STABLE", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Device ID must not be null");
    }

    @Test
    void shouldThrowExceptionWhenUptimeIsNull() {
        assertThatThrownBy(() -> new EvaluateTelemetryCommand(
                sampleDeviceId, sampleTime, null, sampleAirQuality, samplePM,
                sampleConnectivity, sampleLocation, 85, "STABLE", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("uptime must not be null or negative");
    }

    @Test
    void shouldThrowExceptionWhenUptimeIsNegative() {
        assertThatThrownBy(() -> new EvaluateTelemetryCommand(
                sampleDeviceId, sampleTime, -1L, sampleAirQuality, samplePM,
                sampleConnectivity, sampleLocation, 85, "STABLE", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("uptime must not be null or negative");
    }

    @Test
    void shouldThrowExceptionWhenHealthStatusIsNegative() {
        assertThatThrownBy(() -> new EvaluateTelemetryCommand(
                sampleDeviceId, sampleTime, 3600L, sampleAirQuality, samplePM,
                sampleConnectivity, sampleLocation, -1, "STABLE", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("healthStatus must be between 0 and 100");
    }

    @Test
    void shouldThrowExceptionWhenHealthStatusIsGreaterThan100() {
        assertThatThrownBy(() -> new EvaluateTelemetryCommand(
                sampleDeviceId, sampleTime, 3600L, sampleAirQuality, samplePM,
                sampleConnectivity, sampleLocation, 101, "STABLE", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("healthStatus must be between 0 and 100");
    }

    @Test
    void shouldThrowExceptionWhenStatusIsNull() {
        assertThatThrownBy(() -> new EvaluateTelemetryCommand(
                sampleDeviceId, sampleTime, 3600L, sampleAirQuality, samplePM,
                sampleConnectivity, sampleLocation, 85, null, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("status must not be null or blank");
    }
}
