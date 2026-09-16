package com.claircore.alerting.application.commandservices;

import com.claircore.alerting.domain.model.commands.EvaluateTelemetryForAlertsCommand;

/** Inbound port for the alerting write side. */
public interface AlertCommandService {
    void handle(EvaluateTelemetryForAlertsCommand command);
}
