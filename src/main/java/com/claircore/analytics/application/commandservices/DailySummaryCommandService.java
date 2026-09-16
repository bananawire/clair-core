package com.claircore.analytics.application.commandservices;

import com.claircore.analytics.domain.model.commands.GenerateDailySummaryCommand;

public interface DailySummaryCommandService {

    /** @return how many summaries were written; devices already summarised are skipped. */
    int handle(GenerateDailySummaryCommand command);
}
