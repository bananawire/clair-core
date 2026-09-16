package com.claircore.analytics.domain.services;

import com.claircore.analytics.domain.model.valueobjects.MetricTrend;

/**
 * Domain service: expresses the change between two measurements as a percentage.
 *
 * <p>Concrete for the same reason as {@link AqiCalculator} — a pure function has nothing to hide
 * behind an interface.
 */
public class TrendAnalyzer {

    public MetricTrend calculateTrend(Double currentValue, Double previousValue) {
        if (currentValue == null || previousValue == null) {
            return new MetricTrend(currentValue, previousValue, null);
        }
        if (previousValue == 0.0) {
            return new MetricTrend(currentValue, 0.0, null);
        }
        double delta = ((currentValue - previousValue) / Math.abs(previousValue)) * 100.0;
        double rounded = Math.round(delta * 100.0) / 100.0;
        return new MetricTrend(currentValue, previousValue, rounded);
    }
}
