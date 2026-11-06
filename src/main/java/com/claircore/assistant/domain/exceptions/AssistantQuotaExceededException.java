package com.claircore.assistant.domain.exceptions;

import com.claircore.assistant.domain.model.valueobjects.DailyUsage;

/**
 * Raised when the user has already sent every message their plan allows today. The language model
 * is never called in that case. Transport adapters decide how to represent this failure.
 */
public class AssistantQuotaExceededException extends RuntimeException {

    private final transient DailyUsage usage;

    public AssistantQuotaExceededException(DailyUsage usage) {
        super("Daily assistant message limit reached");
        this.usage = usage;
    }

    public DailyUsage getUsage() {
        return usage;
    }
}
