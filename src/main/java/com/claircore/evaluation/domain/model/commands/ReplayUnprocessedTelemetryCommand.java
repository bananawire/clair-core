package com.claircore.evaluation.domain.model.commands;

import java.time.Instant;

/** Re-publishes readings whose alert evaluation never completed and were stored before {@code createdBefore}. */
public record ReplayUnprocessedTelemetryCommand(Instant createdBefore, int limit) {
    public ReplayUnprocessedTelemetryCommand {
        if (createdBefore == null) {
            throw new IllegalArgumentException("createdBefore must not be null");
        }
        if (limit < 1 || limit > 1000) {
            throw new IllegalArgumentException("limit must be between 1 and 1000");
        }
    }
}
