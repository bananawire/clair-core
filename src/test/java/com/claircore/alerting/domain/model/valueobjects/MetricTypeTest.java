package com.claircore.alerting.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class MetricTypeTest {

    @Test
    void includesHydrologyMetricsWithUnits() {
        assertThat(MetricType.RAINFALL.label()).isEqualTo("Rainfall");
        assertThat(MetricType.RAINFALL.unit()).isEqualTo("mm");
        assertThat(MetricType.WATER_LEVEL.label()).isEqualTo("Water level");
        assertThat(MetricType.WATER_LEVEL.unit()).isEqualTo("cm");
    }

    @Test
    void doesNotIncludeBatteryOrSolarAsAlertableMetrics() {
        Set<String> names = Arrays.stream(MetricType.values()).map(Enum::name).collect(Collectors.toSet());
        assertThat(names).doesNotContain("BATTERY", "BATTERY_PERCENT", "SOLAR", "SOLAR_CHARGING");
        assertThat(names).contains("RAINFALL", "WATER_LEVEL", "PM25", "CO2", "TEMPERATURE", "HUMIDITY");
    }
}
