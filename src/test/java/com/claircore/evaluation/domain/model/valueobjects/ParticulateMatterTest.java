package com.claircore.evaluation.domain.model.valueobjects;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParticulateMatterTest {

    @Test
    void shouldCreateParticulateMatterWhenValuesAreValid() {
        // Arrange & Act
        ParticulateMatter particulateMatter = new ParticulateMatter(12.0, 18.0, 35.0);

        // Assert
        assertThat(particulateMatter.pm1_0()).isEqualTo(12);
        assertThat(particulateMatter.pm2_5()).isEqualTo(18);
        assertThat(particulateMatter.pm10()).isEqualTo(35);
    }

    @Test
    void shouldThrowExceptionWhenPm1_0IsNull() {
        // Arrange, Act & Assert
        assertThatThrownBy(() -> new ParticulateMatter(null, 18.0, 35.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pm1_0 must be finite");
    }

    @Test
    void shouldThrowExceptionWhenPm2_5IsNull() {
        // Arrange, Act & Assert
        assertThatThrownBy(() -> new ParticulateMatter(12.0, null, 35.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pm2_5 must be finite");
    }

    @Test
    void shouldThrowExceptionWhenPm10IsNull() {
        // Arrange, Act & Assert
        assertThatThrownBy(() -> new ParticulateMatter(12.0, 18.0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pm10 must be finite");
    }
    @Test void preservesPrecisionAndRejectsInvalidParticles() {
        assertThat(new ParticulateMatter(1.25, 12.45, 20.75).pm2_5()).isEqualTo(12.45);
        for (double value : new double[]{-1, 10001, Double.NaN, Double.POSITIVE_INFINITY})
            assertThatThrownBy(() -> new ParticulateMatter(1.0, value, 20.0)).isInstanceOf(IllegalArgumentException.class);
    }
}
