package com.claircore.device.domain.model.valueobjects;

public enum MetricThreshold {
    PM25("PM2.5", "µg/m³"),
    CO2("CO2", "ppm"),
    TEMPERATURE("Temperature", "°C"),
    HUMIDITY("Humidity", "%"),
    RAINFALL("Rainfall", "mm"),
    WATER_LEVEL("Water level", "cm");

    private final String label;
    private final String unit;

    MetricThreshold(String label, String unit) {
        this.label = label;
        this.unit = unit;
    }

    public String label() { return label; }
    public String unit() { return unit; }
}
