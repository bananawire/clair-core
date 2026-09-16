package com.claircore.analytics.application.internal.queryservices;

import com.claircore.analytics.application.queryservices.KpiHistoricalTrendQueryService;
import com.claircore.analytics.domain.model.queries.GetHistoricalTrendQuery;
import com.claircore.analytics.domain.model.valueobjects.KpiTrendPoint;
import com.claircore.analytics.domain.model.valueobjects.TrendPeriod;
import com.claircore.analytics.domain.repositories.DeviceAnalyticsSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class KpiHistoricalTrendQueryServiceImpl implements KpiHistoricalTrendQueryService {

    private static final Duration DEFAULT_WINDOW = Duration.ofDays(1);

    private final DeviceAnalyticsSnapshotRepository snapshotRepository;

    public KpiHistoricalTrendQueryServiceImpl(DeviceAnalyticsSnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KpiTrendPoint> handle(GetHistoricalTrendQuery query) {
        boolean hasExplicitWindow = query.startDate() != null && query.endDate() != null;
        Instant end = hasExplicitWindow ? query.endDate() : Instant.now();
        Instant start = hasExplicitWindow ? query.startDate() : end.minus(windowOf(query.period()));

        return snapshotRepository
                .findByDeviceIdAndWindowStartBetween(query.deviceId().value(), start, end, query.limit())
                .stream()
                .map(s -> new KpiTrendPoint(
                        s.getTimeWindowStart(),
                        (double) s.getCalculatedAqi().value(),
                        s.getAverageCo2(),
                        s.getAveragePm2_5(),
                        s.getAverageTemperature(),
                        s.getAverageHumidity()
                ))
                .toList();
    }

    /** LIVE has no stored snapshots of its own, so it reads the same window as DAY. */
    private static Duration windowOf(TrendPeriod period) {
        if (period == null) return DEFAULT_WINDOW;
        return switch (period) {
            case LIVE, DAY -> DEFAULT_WINDOW;
            case WEEK -> Duration.ofDays(7);
            case MONTH -> Duration.ofDays(30);
        };
    }
}
