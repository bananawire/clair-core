package com.claircore.evaluation.domain.model.valueobjects;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AirQualityTest {

    @Test
    void shouldCreateAirQualityWhenValuesAreValid() {
        // Arrange & Act
        AirQuality airQuality = new AirQuality(450.0, 23.5, 50.0);

        // Assert
        assertThat(airQuality.co2()).isEqualTo(450.0);
        assertThat(airQuality.temperature()).isEqualTo(23.5);
        assertThat(airQuality.humidity()).isEqualTo(50.0);
    }

    @Test
    void shouldThrowExceptionWhenCo2IsNull() {
        // Arrange, Act & Assert
        assertThatThrownBy(() -> new AirQuality(null, 23.5, 50.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("co2 must be finite");
    }

    @Test
    void shouldThrowExceptionWhenTemperatureIsNull() {
        // Arrange, Act & Assert
        assertThatThrownBy(() -> new AirQuality(450.0, null, 50.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("temperature must be finite");
    }

    @Test
    void shouldThrowExceptionWhenHumidityIsNull() {
        // Arrange, Act & Assert
        assertThatThrownBy(() -> new AirQuality(450.0, 23.5, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("humidity must be finite");
    }
    @Test void rejectsBrokenSensorValues() {
        for (double value : new double[]{-1, 1000001, Double.NaN, Double.POSITIVE_INFINITY})
            assertThatThrownBy(() -> new AirQuality(value, 22.0, 50.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AirQuality(400.0, 9999.0, 50.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AirQuality(400.0, 22.0, 101.0)).isInstanceOf(IllegalArgumentException.class);
    }
}
