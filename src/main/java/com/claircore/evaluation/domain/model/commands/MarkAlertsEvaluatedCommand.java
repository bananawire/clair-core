package com.claircore.evaluation.domain.model.commands;

import java.time.Instant;
import java.util.UUID;

/** Alerting finished with reading {@code readingId} of {@code deviceId} at {@code evaluatedAt}. */
public record MarkAlertsEvaluatedCommand(UUID deviceId, UUID readingId, Instant evaluatedAt) {
    public MarkAlertsEvaluatedCommand {
        if (deviceId == null || readingId == null) {
            throw new IllegalArgumentException("deviceId and readingId must not be null");
        }
        if (evaluatedAt == null) {
            evaluatedAt = Instant.now();
        }
    }
}
