package com.claircore.evaluation.interfaces.acl;

import java.time.Instant;
import java.util.UUID;

/**
 * Outcome of {@code EvaluationContextFacade#recordTelemetry}. {@code accepted=true} means the
 * reading was persisted (possibly as a no-op if {@code readingId} already existed);
 * {@code accepted=false} means the Evaluation BC rejected the submission for a domain reason
 * (validation, rate limiting, etc.) without throwing.
 */
public record TelemetryRecordingResult(
        UUID readingId,
        Instant recordedAt,
        boolean accepted
) {
    public static TelemetryRecordingResult rejected() {
        return new TelemetryRecordingResult(null, null, false);
    }

    public static TelemetryRecordingResult accepted(UUID readingId, Instant recordedAt) {
        return new TelemetryRecordingResult(readingId, recordedAt, true);
    }
}
