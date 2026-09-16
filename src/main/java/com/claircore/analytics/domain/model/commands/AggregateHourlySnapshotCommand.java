package com.claircore.analytics.domain.model.commands;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Snapshot every device's averages over the hour ending at {@code windowEnd}, which must sit on an
 * hour boundary so consecutive snapshots tile the timeline without gaps or overlap.
 */
public record AggregateHourlySnapshotCommand(Instant windowEnd) {
    public AggregateHourlySnapshotCommand {
        if (windowEnd == null) {
            throw new IllegalArgumentException("windowEnd must not be null");
        }
        if (!windowEnd.equals(windowEnd.truncatedTo(ChronoUnit.HOURS))) {
            throw new IllegalArgumentException("windowEnd must fall on an hour boundary");
        }
    }

    public Instant windowStart() {
        return windowEnd.minus(1, ChronoUnit.HOURS);
    }
}
