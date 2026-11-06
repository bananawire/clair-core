package com.claircore.assistant.domain.model.valueobjects;

import java.time.Instant;

/**
 * How many assistant messages a user has sent today against their plan's allowance.
 *
 * @param plan     name of the effective plan the limit comes from
 * @param used     user messages sent since the start of the current day
 * @param limit    daily allowance of the plan
 * @param resetsAt instant the next day starts and {@code used} goes back to zero
 */
public record DailyUsage(String plan, int used, int limit, Instant resetsAt) {

    public DailyUsage {
        if (plan == null || plan.isBlank()) throw new IllegalArgumentException("Plan is required");
        if (used < 0) throw new IllegalArgumentException("Used must not be negative");
        if (limit < 0) throw new IllegalArgumentException("Limit must not be negative");
        if (resetsAt == null) throw new IllegalArgumentException("Resets at is required");
    }

    public int remaining() {
        return Math.max(0, limit - used);
    }

    public boolean isExhausted() {
        return used >= limit;
    }

    /** The same day after one more user message. */
    public DailyUsage withOneMoreMessage() {
        return new DailyUsage(plan, used + 1, limit, resetsAt);
    }
}
