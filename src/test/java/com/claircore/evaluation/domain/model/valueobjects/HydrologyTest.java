package com.claircore.evaluation.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HydrologyTest {

    @Test
    void acceptsValidRainfallAndWaterLevel() {
        Hydrology h = new Hydrology(1.25, 42.0);
        assertThat(h.rainfallMm()).isEqualTo(1.25);
        assertThat(h.waterLevelCm()).isEqualTo(42.0);
    }

    @Test
    void absentDefaultsToDryZero() {
        assertThat(Hydrology.absent()).isEqualTo(new Hydrology(0.0, 0.0));
    }

    @Test
    void rejectsNegativeRainfall() {
        assertThatThrownBy(() -> new Hydrology(-0.1, 10.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rainfallMm");
    }

    @Test
    void rejectsNegativeWaterLevel() {
        assertThatThrownBy(() -> new Hydrology(0.0, -1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("waterLevelCm");
    }
}
