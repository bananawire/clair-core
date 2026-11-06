package com.claircore.evaluation.domain.model.valueobjects;

/**
 * Outdoor node power status for one telemetry sample.
 *
 * <p>Status-only: never mapped into {@code MetricType} / device thresholds.
 * {@code batteryPercent} may be null when the node has no battery monitor
 * (typical indoor inventory). {@code solarCharging} is false when omitted.
 */
public record PowerStatus(Double batteryPercent, boolean solarCharging) {
    public PowerStatus {
        if (batteryPercent != null
                && (!Double.isFinite(batteryPercent) || batteryPercent < 0 || batteryPercent > 100)) {
            throw new IllegalArgumentException("batteryPercent must be null or finite within [0, 100]");
        }
    }

    /** Indoor / legacy readings that omit power report no battery and not charging. */
    public static PowerStatus absent() {
        return new PowerStatus(null, false);
    }
}
