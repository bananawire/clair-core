package com.claircore.evaluation.application.internal.schedulers;

import com.claircore.evaluation.application.commandservices.TelemetryEvaluationCommandService;
import com.claircore.evaluation.domain.model.commands.ReplayUnprocessedTelemetryCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Replays readings whose alert evaluation never completed (a crash after the reading committed).
 * Waits a grace period so it never races the normal after-commit path.
 */
@Component
public class AlertEvaluationCatchUpScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlertEvaluationCatchUpScheduler.class);
    private static final int BATCH = 200;

    private final TelemetryEvaluationCommandService commandService;
    private final Duration grace;

    public AlertEvaluationCatchUpScheduler(TelemetryEvaluationCommandService commandService,
                                           @Value("${claircore.alerts.catch-up-grace-seconds:30}") long graceSeconds) {
        this.commandService = commandService;
        this.grace = Duration.ofSeconds(graceSeconds);
    }

    @Scheduled(fixedDelayString = "${claircore.alerts.catch-up-interval-ms:60000}", initialDelayString = "${claircore.alerts.catch-up-initial-delay-ms:15000}")
    public void replayUnprocessed() {
        int replayed = commandService.handle(new ReplayUnprocessedTelemetryCommand(Instant.now().minus(grace), BATCH));
        if (replayed > 0) {
            LOGGER.warn("Replayed alert evaluation for {} reading(s) that were never processed", replayed);
        }
    }
}
