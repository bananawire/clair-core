package com.claircore.evaluation.application.internal.commandservices;

import com.claircore.evaluation.application.commandservices.TelemetryEvaluationCommandService;
import com.claircore.evaluation.domain.model.aggregates.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.commands.EvaluateTelemetryCommand;
import com.claircore.evaluation.domain.model.commands.MarkAlertsEvaluatedCommand;
import com.claircore.evaluation.domain.model.commands.ReplayUnprocessedTelemetryCommand;
import com.claircore.evaluation.domain.repositories.TelemetryEvaluationRepository;
import com.claircore.evaluation.interfaces.events.TelemetryRecordedIntegrationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelemetryEvaluationCommandServiceImpl implements TelemetryEvaluationCommandService {

    private final TelemetryEvaluationRepository telemetryEvaluationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TelemetryEvaluationCommandServiceImpl(
            TelemetryEvaluationRepository telemetryEvaluationRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.telemetryEvaluationRepository = telemetryEvaluationRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public TelemetryEvaluation handle(EvaluateTelemetryCommand command) {
        var evaluation = new TelemetryEvaluation(
                command.deviceId(),
                command.readingId(),
                command.uptime(),
                command.airQuality(),
                command.particulateMatter(),
                command.connectivity(),
                command.location(),
                command.healthStatus(),
                command.status(),
                command.recordedAt()
        );

        var result = telemetryEvaluationRepository.saveIfAbsent(evaluation);
        TelemetryEvaluation saved = result.reading();
        if (!result.inserted()) {
            // Identity is immutable. Reusing it for different measurements is a client error.
            if (!saved.getRecordedAt().equals(evaluation.getRecordedAt())
                    || !saved.getAirQuality().equals(evaluation.getAirQuality())
                    || !saved.getParticulateMatter().equals(evaluation.getParticulateMatter())
                    || !saved.getUptime().equals(evaluation.getUptime())
                    || !saved.getConnectivity().equals(evaluation.getConnectivity())
                    || !saved.getLocation().equals(evaluation.getLocation())
                    || !saved.getHealthStatus().equals(evaluation.getHealthStatus())
                    || !saved.getStatus().equals(evaluation.getStatus())) {
                throw new IllegalArgumentException("readingId already exists with different measurement data");
            }
            return saved;
        }

        // The published contract, and now the only telemetry event: alerting and analytics both
        // listen to it, so the internal event this used to be published alongside is gone.
        eventPublisher.publishEvent(TelemetryRecordedIntegrationEvent.from(saved));

        return saved;
    }
    /**
     * Runs inside a transaction on purpose: the consumers are after-commit listeners, so the
     * replayed events fire exactly like the originals once this transaction commits.
     */
    @Override
    @Transactional
    public int handle(ReplayUnprocessedTelemetryCommand command) {
        var pending = telemetryEvaluationRepository.findAlertsPending(command.createdBefore(), command.limit());
        for (TelemetryEvaluation reading : pending) {
            eventPublisher.publishEvent(TelemetryRecordedIntegrationEvent.from(reading));
        }
        return pending.size();
    }

    @Override
    @Transactional
    public void handle(MarkAlertsEvaluatedCommand command) {
        telemetryEvaluationRepository.markAlertsEvaluated(command.deviceId(), command.readingId(), command.evaluatedAt());
    }
}
