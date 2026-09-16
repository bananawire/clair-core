package com.claircore.evaluation.domain.model.valueobjects;

public record ParticulateMatter(Double pm1_0, Double pm2_5, Double pm10) {
    public ParticulateMatter {
        requireValid(pm1_0, "pm1_0");
        requireValid(pm2_5, "pm2_5");
        requireValid(pm10, "pm10");
    }
    private static void requireValid(Double value, String name) {
        if (value == null || !Double.isFinite(value) || value < 0 || value > 10_000)
            throw new IllegalArgumentException(name + " must be finite and between 0 and 10000 ug/m3 (indoor product range)");
    }
}
