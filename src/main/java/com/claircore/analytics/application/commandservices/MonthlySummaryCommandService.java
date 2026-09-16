package com.claircore.analytics.application.commandservices;

import com.claircore.analytics.domain.model.commands.GenerateMonthlySummaryCommand;

public interface MonthlySummaryCommandService {

    /** @return how many summaries were written; months already summarised are skipped. */
    int handle(GenerateMonthlySummaryCommand command);
}
