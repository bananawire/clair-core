package com.claircore.device.interfaces.acl;

import java.math.BigDecimal;

/**
 * A configured metric threshold, as other contexts see it.
 *
 * <p>The metric is a {@code String}, not device's {@code MetricThreshold} enum: a consumer that
 * names another context's enum is coupled to its storage mapping, and alerting has a metric enum
 * of its own to map onto.
 */
public record ThresholdSummary(String metric, BigDecimal value, boolean enabled) {
    public ThresholdSummary {
        if (metric == null || metric.isBlank()) throw new IllegalArgumentException("Metric must not be null or blank");
        if (value == null) throw new IllegalArgumentException("Value must not be null");
    }
}
