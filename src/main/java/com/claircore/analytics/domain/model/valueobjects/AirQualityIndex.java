package com.claircore.analytics.domain.model.valueobjects;

public record AirQualityIndex(
        Integer value,
        AqiCategory category
) {
    public AirQualityIndex {
        if (value == null || value < 0 || value > 500) {
            throw new IllegalArgumentException("AQI value must be between 0 and 500");
        }
        if (category == null) {
            throw new IllegalArgumentException("AQI category must not be null");
        }
    }
}
