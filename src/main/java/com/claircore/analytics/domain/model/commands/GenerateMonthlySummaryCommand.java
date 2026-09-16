package com.claircore.analytics.domain.model.commands;

import java.time.YearMonth;

/** Cascade a month's daily summaries into one monthly summary per device. */
public record GenerateMonthlySummaryCommand(YearMonth month) {
    public GenerateMonthlySummaryCommand {
        if (month == null) {
            throw new IllegalArgumentException("month must not be null");
        }
    }
}
