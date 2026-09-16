package com.claircore.analytics.domain.model.valueobjects;

/**
 * The window a dashboard or trend request covers. {@code LIVE} is the in-memory buffer of the last
 * few minutes; the rest are read from stored snapshots.
 */
public enum TrendPeriod {
    LIVE,
    DAY,
    WEEK,
    MONTH
}
