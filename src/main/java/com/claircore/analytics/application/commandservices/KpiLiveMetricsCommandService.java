package com.claircore.analytics.application.commandservices;

import com.claircore.analytics.domain.model.commands.ProcessTelemetryAnalyticCommand;

public interface KpiLiveMetricsCommandService {

    void handle(ProcessTelemetryAnalyticCommand command);
}
