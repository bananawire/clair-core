package com.claircore.analytics.domain.model.commands;

import java.time.LocalDate;

/** Summarise every device's raw telemetry for one calendar day in the report zone. */
public record GenerateDailySummaryCommand(LocalDate date) {
    public GenerateDailySummaryCommand {
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
        }
    }
}
