package com.claircore.analytics.domain.model.valueobjects;

public record MetricTrend(
        Double currentValue,
        Double previousValue,
        Double deltaPercentage
) {
    public MetricTrend {
        if (currentValue == null) {
            throw new IllegalArgumentException("currentValue must not be null");
        }
    }
}
