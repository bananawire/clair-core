package com.claircore.alerting.application.internal.eventhandlers;

import com.claircore.alerting.application.commandservices.AlertCommandService;
import com.claircore.alerting.application.internal.outboundservices.acl.ExternalAlertingEvaluationService;
import java.util.UUID;
import com.claircore.alerting.domain.model.commands.EvaluateTelemetryForAlertsCommand;
import com.claircore.evaluation.interfaces.events.TelemetryRecordedIntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Turns evaluation's published telemetry event into an alert evaluation. */
@Component("alertingTelemetryRecordedEventHandler")
public class TelemetryRecordedEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryRecordedEventHandler.class);

    private final AlertCommandService alertCommandService;
    private final ExternalAlertingEvaluationService externalEvaluationService;

    public TelemetryRecordedEventHandler(AlertCommandService alertCommandService,
                                         ExternalAlertingEvaluationService externalEvaluationService) {
        this.alertCommandService = alertCommandService;
        this.externalEvaluationService = externalEvaluationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void on(TelemetryRecordedIntegrationEvent event) {
        LOGGER.info("Alerting BC received telemetry recorded event for device {}", event.deviceId());
        try {
            alertCommandService.handle(new EvaluateTelemetryForAlertsCommand(
                    event.deviceId(),
                    event.recordedAt(),
                    BigDecimal.valueOf(event.pm2_5()),
                    BigDecimal.valueOf(event.co2()),
                    BigDecimal.valueOf(event.temperature()),
                    BigDecimal.valueOf(event.humidity())
            ));
            // The receipt is written only after a successful evaluation; a failure leaves it null so the
            // catch-up scheduler replays this reading instead of losing it.
            externalEvaluationService.markAlertsEvaluated(event.deviceId(), UUID.fromString(event.readingId()));
        } catch (Exception e) {
            LOGGER.error("Failed to process telemetry event for alerting, device {}", event.deviceId(), e);
        }
    }
}
