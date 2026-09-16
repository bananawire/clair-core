package com.claircore.analytics.application.internal.queryservices;

import com.claircore.analytics.application.internal.outboundservices.cache.LiveMetricsStore;
import com.claircore.analytics.application.queryservices.KpiDashboardMetricsQueryService;
import com.claircore.shared.domain.exceptions.ResourceNotFoundException;
import com.claircore.analytics.domain.model.queries.GetDashboardMetricsQuery;
import com.claircore.analytics.domain.model.valueobjects.KpiDashboardMetrics;
import com.claircore.analytics.domain.model.valueobjects.MetricAverages;
import com.claircore.analytics.domain.model.valueobjects.TrendPeriod;
import com.claircore.analytics.domain.repositories.DeviceAnalyticsSnapshotRepository;
import com.claircore.analytics.domain.services.AqiCalculator;
import com.claircore.analytics.domain.services.TrendAnalyzer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class KpiDashboardMetricsQueryServiceImpl implements KpiDashboardMetricsQueryService {

    private static final Duration DEFAULT_WINDOW = Duration.ofDays(1);

    private final LiveMetricsStore liveMetricsStore;
    private final AqiCalculator aqiCalculator;
    private final TrendAnalyzer trendAnalyzer;
    private final DeviceAnalyticsSnapshotRepository snapshotRepository;
    private final com.claircore.analytics.application.internal.outboundservices.acl.ExternalEvaluationService evaluations;

    public KpiDashboardMetricsQueryServiceImpl(
            LiveMetricsStore liveMetricsStore,
            AqiCalculator aqiCalculator,
            TrendAnalyzer trendAnalyzer,
            DeviceAnalyticsSnapshotRepository snapshotRepository,
            com.claircore.analytics.application.internal.outboundservices.acl.ExternalEvaluationService evaluations
    ) {
        this.liveMetricsStore = liveMetricsStore;
        this.aqiCalculator = aqiCalculator;
        this.trendAnalyzer = trendAnalyzer;
        this.snapshotRepository = snapshotRepository;
        this.evaluations = evaluations;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<KpiDashboardMetrics> handle(GetDashboardMetricsQuery query) {
        boolean hasExplicitWindow = query.startDate() != null && query.endDate() != null;
        if (!hasExplicitWindow && (query.period() == null || query.period() == TrendPeriod.LIVE)) {
            return liveMetrics(query.deviceId().value());
        }
        return Optional.of(historicalMetrics(query, hasExplicitWindow));
    }

    private Optional<KpiDashboardMetrics> liveMetrics(UUID deviceId) {
        var buffer = liveMetricsStore.getIfPresent(deviceId);
        if (buffer == null) {
            return Optional.empty();
        }

        var window = buffer.computeAverages();
        if (window.isEmpty()) return Optional.empty();
        var avg = window.orElseThrow();
        var aqi = aqiCalculator.calculateAqi(avg.pm2_5());

        // The last stored snapshot is the "previous" the live window is compared against; with none,
        // the trend compares the window to itself and reads as flat rather than as a spike.
        var latest = snapshotRepository.findLatestByDeviceId(deviceId);

        return Optional.of(new KpiDashboardMetrics(
                aqi,
                avg.co2(),
                avg.pm2_5(),
                avg.temperature(),
                avg.humidity(),
                trendAnalyzer.calculateTrend(avg.co2(),
                        latest.map(s -> s.getAverageCo2()).orElse(avg.co2())),
                trendAnalyzer.calculateTrend(avg.pm2_5(),
                        latest.map(s -> s.getAveragePm2_5()).orElse(avg.pm2_5())),
                trendAnalyzer.calculateTrend(avg.temperature(),
                        latest.map(s -> s.getAverageTemperature()).orElse(avg.temperature())),
                trendAnalyzer.calculateTrend(avg.humidity(),
                        latest.map(s -> s.getAverageHumidity()).orElse(avg.humidity())),
                avg.measuredAt()
        ));
    }

    private KpiDashboardMetrics historicalMetrics(GetDashboardMetricsQuery query, boolean hasExplicitWindow) {
        UUID deviceId = query.deviceId().value();
        Instant end = hasExplicitWindow ? query.endDate() : Instant.now();
        Instant start = hasExplicitWindow ? query.startDate() : end.minus(windowOf(query.period()));

        var averages = periodAverages(deviceId, start, end)
                .orElseThrow(() -> new ResourceNotFoundException("No telemetry data available for device with ID %s in the requested period.".formatted(deviceId)));
        var aqi = aqiCalculator.calculateAqi(averages.pm2_5());

        // Trends compare the window against the window of equal length that precedes it.
        Duration duration = Duration.between(start, end);
        var previous = periodAverages(deviceId, start.minus(duration), start);

        return new KpiDashboardMetrics(
                aqi,
                averages.co2(),
                averages.pm2_5(),
                averages.temperature(),
                averages.humidity(),
                trendAnalyzer.calculateTrend(averages.co2(), previous.map(MetricAverages::co2).orElse(null)),
                trendAnalyzer.calculateTrend(averages.pm2_5(), previous.map(MetricAverages::pm2_5).orElse(null)),
                trendAnalyzer.calculateTrend(averages.temperature(), previous.map(MetricAverages::temperature).orElse(null)),
                trendAnalyzer.calculateTrend(averages.humidity(), previous.map(MetricAverages::humidity).orElse(null)),
                Instant.now()
        );
    }

    private Optional<MetricAverages> periodAverages(UUID deviceId, Instant start, Instant end) {
        return evaluations.fetchHourlyTelemetryAggregation(start, end).stream()
                .filter(row -> row.deviceId().equals(deviceId))
                .findFirst().map(row -> new MetricAverages(row.averageCo2(), row.averagePm25(),
                        row.averageTemperature(), row.averageHumidity()));
    }

    private static Duration windowOf(TrendPeriod period) {
        if (period == null) return DEFAULT_WINDOW;
        return switch (period) {
            case LIVE, DAY -> DEFAULT_WINDOW;
            case WEEK -> Duration.ofDays(7);
            case MONTH -> Duration.ofDays(30);
        };
    }
}
