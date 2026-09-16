package com.claircore.evaluation.application.commandservices;

import com.claircore.evaluation.domain.model.aggregates.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.commands.EvaluateTelemetryCommand;
import com.claircore.evaluation.domain.model.commands.MarkAlertsEvaluatedCommand;
import com.claircore.evaluation.domain.model.commands.ReplayUnprocessedTelemetryCommand;

public interface TelemetryEvaluationCommandService {
    TelemetryEvaluation handle(EvaluateTelemetryCommand command);
    /** @return how many readings were re-published */
    int handle(ReplayUnprocessedTelemetryCommand command);
    void handle(MarkAlertsEvaluatedCommand command);
}
