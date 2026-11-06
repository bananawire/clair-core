package com.claircore.device.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetricThresholdTest {

    @Test
    void shouldExposeLabelAndUnitForPm25() {
        assertEquals("PM2.5", MetricThreshold.PM25.label());
        assertEquals("µg/m³", MetricThreshold.PM25.unit());
    }

    @Test
    void shouldExposeLabelAndUnitForCo2() {
        assertEquals("CO2", MetricThreshold.CO2.label());
        assertEquals("ppm", MetricThreshold.CO2.unit());
    }

    @Test
    void shouldExposeLabelAndUnitForTemperature() {
        assertEquals("Temperature", MetricThreshold.TEMPERATURE.label());
        assertEquals("°C", MetricThreshold.TEMPERATURE.unit());
    }

    @Test
    void shouldExposeLabelAndUnitForHumidity() {
        assertEquals("Humidity", MetricThreshold.HUMIDITY.label());
        assertEquals("%", MetricThreshold.HUMIDITY.unit());
    }

    @Test
    void shouldExposeLabelAndUnitForRainfall() {
        assertEquals("Rainfall", MetricThreshold.RAINFALL.label());
        assertEquals("mm", MetricThreshold.RAINFALL.unit());
    }

    @Test
    void shouldExposeLabelAndUnitForWaterLevel() {
        assertEquals("Water level", MetricThreshold.WATER_LEVEL.label());
        assertEquals("cm", MetricThreshold.WATER_LEVEL.unit());
    }
}
