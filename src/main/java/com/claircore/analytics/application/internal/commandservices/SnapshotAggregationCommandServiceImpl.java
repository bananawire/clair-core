package com.claircore.analytics.application.internal.commandservices;

import com.claircore.analytics.application.commandservices.SnapshotAggregationCommandService;
import com.claircore.analytics.application.internal.outboundservices.acl.ExternalEvaluationService;
import com.claircore.analytics.domain.model.aggregates.DeviceAnalyticsSnapshot;
import com.claircore.analytics.domain.model.commands.AggregateHourlySnapshotCommand;
import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.domain.repositories.DeviceAnalyticsSnapshotRepository;
import com.claircore.analytics.domain.services.AqiCalculator;
import com.claircore.evaluation.interfaces.acl.HourlyTelemetryAverage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Snapshots each reporting device's averages for one closed hour, with the AQI derived from them. */
@Service
public class SnapshotAggregationCommandServiceImpl implements SnapshotAggregationCommandService {

    private final ExternalEvaluationService externalEvaluationService;
    private final DeviceAnalyticsSnapshotRepository snapshotRepository;
    private final AqiCalculator aqiCalculator;

    public SnapshotAggregationCommandServiceImpl(
            ExternalEvaluationService externalEvaluationService,
            DeviceAnalyticsSnapshotRepository snapshotRepository,
            AqiCalculator aqiCalculator
    ) {
        this.externalEvaluationService = externalEvaluationService;
        this.snapshotRepository = snapshotRepository;
        this.aqiCalculator = aqiCalculator;
    }

    @Override
    @Transactional
    public int handle(AggregateHourlySnapshotCommand command) {
        List<HourlyTelemetryAverage> rows = externalEvaluationService
                .fetchHourlyTelemetryAggregation(command.windowStart(), command.windowEnd());

        for (HourlyTelemetryAverage row : rows) {
            var aqi = aqiCalculator.calculateAqi(row.averagePm25());
            snapshotRepository.save(new DeviceAnalyticsSnapshot(
                    new DeviceId(row.deviceId()),
                    command.windowStart(),
                    command.windowEnd(),
                    row.averageCo2(),
                    row.averagePm25(),
                    row.averageTemperature(),
                    row.averageHumidity(),
                    aqi
            ));
        }
        return rows.size();
    }
}
