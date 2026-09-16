package com.claircore.evaluation.domain.model.valueobjects;

public record AirQuality(Double co2, Double temperature, Double humidity) {
    public AirQuality {
        if (co2 == null || !Double.isFinite(co2) || co2 < 0 || co2 > 1_000_000)
            throw new IllegalArgumentException("co2 must be finite and between 0 and 1000000 ppm");
        if (temperature == null || !Double.isFinite(temperature) || temperature < -50 || temperature > 100)
            throw new IllegalArgumentException("temperature must be finite and between -50 and 100 Celsius (indoor product range)");
        if (humidity == null || !Double.isFinite(humidity) || humidity < 0 || humidity > 100)
            throw new IllegalArgumentException("humidity must be finite and between 0 and 100 percent");
    }
}
