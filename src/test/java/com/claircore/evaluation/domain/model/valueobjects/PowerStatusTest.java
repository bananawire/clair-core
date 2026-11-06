package com.claircore.evaluation.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PowerStatusTest {

    @Test
    void acceptsNullBatteryAndChargingFlag() {
        PowerStatus p = new PowerStatus(null, true);
        assertThat(p.batteryPercent()).isNull();
        assertThat(p.solarCharging()).isTrue();
    }

    @Test
    void absentHasNoBatteryAndNotCharging() {
        assertThat(PowerStatus.absent()).isEqualTo(new PowerStatus(null, false));
    }

    @Test
    void rejectsBatteryOutsidePercentRange() {
        assertThatThrownBy(() -> new PowerStatus(120.0, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("batteryPercent");
    }
}
