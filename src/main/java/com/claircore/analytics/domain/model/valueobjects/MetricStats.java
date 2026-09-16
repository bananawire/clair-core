package com.claircore.analytics.domain.model.valueobjects;

/**
 * Average / minimum / maximum of a single metric over a period. The {@code min}
 * and {@code max} are true extremes from the raw readings (not extremes of an
 * average), which is why daily summaries are computed from raw telemetry.
 */
public record MetricStats(
        Double avg,
        Double min,
        Double max
) {
    public static MetricStats of(double avg, double min, double max) {
        return new MetricStats(avg, min, max);
    }
}
