package com.claircore.analytics.application.internal.eventhandlers;

import com.claircore.analytics.application.commandservices.KpiLiveMetricsCommandService;
import com.claircore.analytics.domain.model.commands.ProcessTelemetryAnalyticCommand;
import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.evaluation.interfaces.events.TelemetryRecordedIntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.stereotype.Component;

/** Feeds the live-metrics buffer from evaluation's published telemetry event. */
@Component("analyticsTelemetryRecordedEventHandler")
public class TelemetryRecordedEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryRecordedEventHandler.class);

    private final KpiLiveMetricsCommandService kpiLiveMetricsCommandService;

    public TelemetryRecordedEventHandler(KpiLiveMetricsCommandService kpiLiveMetricsCommandService) {
        this.kpiLiveMetricsCommandService = kpiLiveMetricsCommandService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(TelemetryRecordedIntegrationEvent event) {
        try {
            kpiLiveMetricsCommandService.handle(new ProcessTelemetryAnalyticCommand(
                    new DeviceId(event.deviceId()),
                    event.co2(),
                    (double) event.pm2_5(),
                    event.temperature(),
                    event.humidity(),
                    event.recordedAt()
            ));
        } catch (Exception e) {
            // A rejected reading must not take down the recording that produced it.
            LOGGER.error("Failed to process analytics telemetry event for device {}", event.deviceId(), e);
        }
    }
}
