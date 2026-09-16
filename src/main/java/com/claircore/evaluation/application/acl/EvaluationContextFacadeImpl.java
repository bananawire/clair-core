package com.claircore.evaluation.application.acl;

import com.claircore.evaluation.application.commandservices.TelemetryEvaluationCommandService;
import com.claircore.evaluation.application.queryservices.TelemetryEvaluationQueryService;
import com.claircore.evaluation.domain.model.aggregates.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.commands.EvaluateTelemetryCommand;
import com.claircore.evaluation.domain.model.commands.MarkAlertsEvaluatedCommand;
import com.claircore.evaluation.domain.model.queries.GetDeviceReadingsQuery;
import com.claircore.evaluation.domain.model.queries.GetHourlyTelemetryAveragesQuery;
import com.claircore.evaluation.domain.model.queries.GetLatestEvaluationByDeviceQuery;
import com.claircore.evaluation.domain.model.valueobjects.AirQuality;
import com.claircore.evaluation.domain.model.valueobjects.Connectivity;
import com.claircore.evaluation.domain.model.valueobjects.DeviceId;
import com.claircore.evaluation.domain.model.valueobjects.Location;
import com.claircore.evaluation.domain.model.valueobjects.ParticulateMatter;
import com.claircore.evaluation.interfaces.acl.EvaluationContextFacade;
import com.claircore.evaluation.interfaces.acl.HourlyTelemetryAverage;
import com.claircore.evaluation.interfaces.acl.TelemetryReading;
import com.claircore.evaluation.interfaces.acl.TelemetryRecordingResult;
import com.claircore.evaluation.interfaces.acl.TelemetrySubmission;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EvaluationContextFacadeImpl implements EvaluationContextFacade {

    private final TelemetryEvaluationQueryService telemetryEvaluationQueryService;
    private final TelemetryEvaluationCommandService telemetryEvaluationCommandService;

    public EvaluationContextFacadeImpl(TelemetryEvaluationQueryService telemetryEvaluationQueryService,
                                      TelemetryEvaluationCommandService telemetryEvaluationCommandService) {
        this.telemetryEvaluationQueryService = telemetryEvaluationQueryService;
        this.telemetryEvaluationCommandService = telemetryEvaluationCommandService;
    }

    @Override
    public void markAlertsEvaluated(UUID deviceId, UUID readingId) {
        telemetryEvaluationCommandService.handle(new MarkAlertsEvaluatedCommand(deviceId, readingId, Instant.now()));
    }

    @Override
    public Optional<Instant> getLatestEvaluationRecordedAt(UUID deviceId) {
        var query = new GetLatestEvaluationByDeviceQuery(deviceId);
        return telemetryEvaluationQueryService.handle(query)
                .map(TelemetryEvaluation::getRecordedAt);
    }

    @Override
    public List<HourlyTelemetryAverage> getHourlyTelemetryAggregation(Instant start, Instant end) {
        return telemetryEvaluationQueryService.handle(new GetHourlyTelemetryAveragesQuery(start, end)).stream()
                .map(row -> new HourlyTelemetryAverage(
                        row.deviceId(),
                        row.averageCo2(),
                        row.averagePm25(),
                        row.averageTemperature(),
                        row.averageHumidity()))
                .toList();
    }

    @Override
    public List<TelemetryReading> getReadingsBetween(Instant start, Instant end) {
        return telemetryEvaluationQueryService.handle(new GetDeviceReadingsQuery(start, end)).stream()
                .map(row -> new TelemetryReading(
                        row.deviceId(),
                        row.co2(),
                        row.pm2_5(),
                        row.temperature(),
                        row.humidity(),
                        row.recordedAt()))
                .toList();
    }

    @Override
    public TelemetryRecordingResult recordTelemetry(TelemetrySubmission submission) {
        // Fresh readingId per submission: the LocalEdge ACL owns identity, so two BCs cannot
        // accidentally collide on the Evaluation BC's identity field.
        UUID readingId = UUID.randomUUID();
        EvaluateTelemetryCommand command;
        try {
            command = new EvaluateTelemetryCommand(
                    new DeviceId(submission.deviceId()),
                    readingId,
                    0L,
                    new AirQuality(submission.co2(), submission.temperature(), submission.humidity()),
                    new ParticulateMatter(submission.pm1_0(), submission.pm2_5(), submission.pm10()),
                    new Connectivity(submission.connectivityStatus(), submission.networkName(), submission.signalStrength()),
                    new Location(submission.country()),
                    submission.healthStatus(),
                    submission.status(),
                    submission.recordedAt());
        } catch (IllegalArgumentException ex) {
            // Domain validation fired at the value-object level: surface as a non-throwing rejection
            // so the LocalEdge scheduler can keep counting the cycle outcome without aborting.
            return TelemetryRecordingResult.rejected();
        }
        try {
            TelemetryEvaluation saved = telemetryEvaluationCommandService.handle(command);
            return TelemetryRecordingResult.accepted(saved.getReadingId(), saved.getRecordedAt());
        } catch (IllegalArgumentException ex) {
            return TelemetryRecordingResult.rejected();
        }
    }
}
