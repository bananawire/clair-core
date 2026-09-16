package com.claircore.analytics.application.internal.queryservices;

import com.claircore.alerting.interfaces.acl.AlertDetails;
import com.claircore.alerting.interfaces.acl.AlertingContextFacade;
import com.claircore.analytics.application.internal.outboundservices.acl.ExternalDeviceService;
import com.claircore.analytics.application.internal.outboundservices.cache.KpiLiveMetricsBuffer;
import com.claircore.analytics.application.internal.outboundservices.cache.LiveMetricsStore;
import com.claircore.analytics.application.queryservices.OverviewDashboardQueryService;
import com.claircore.analytics.domain.model.queries.GetOverviewDashboardQuery;
import com.claircore.analytics.domain.model.valueobjects.AggregatedMetrics;
import com.claircore.analytics.domain.model.valueobjects.DeviceMetricsSnapshot;
import com.claircore.analytics.domain.model.valueobjects.Freshness;
import com.claircore.analytics.domain.model.valueobjects.OverviewDashboardSnapshot;
import com.claircore.analytics.domain.services.MetricsAggregator;
import com.claircore.analytics.domain.services.TrendAnalyzer;
import com.claircore.analytics.domain.services.AqiCalculator;
import com.claircore.analytics.domain.repositories.DeviceAnalyticsSnapshotRepository;
import com.claircore.device.interfaces.acl.OrganizationSummary;
import com.claircore.device.interfaces.acl.SpaceSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;


@Service
public class OverviewDashboardQueryServiceImpl implements OverviewDashboardQueryService {

    private static final List<String> DEFAULT_ALERT_STATUSES = List.of("ACTIVE", "ACKNOWLEDGED");

    private final ExternalDeviceService externalDeviceService;
    private final AlertingContextFacade alertingContextFacade;
    private final LiveMetricsStore liveMetricsStore;
    private final DeviceAnalyticsSnapshotRepository snapshotRepository;
    private final MetricsAggregator metricsAggregator;
    private final TrendAnalyzer trendAnalyzer;
    private final AqiCalculator aqiCalculator;

    public OverviewDashboardQueryServiceImpl(
            ExternalDeviceService externalDeviceService,
            AlertingContextFacade alertingContextFacade,
            LiveMetricsStore liveMetricsStore,
            DeviceAnalyticsSnapshotRepository snapshotRepository,
            MetricsAggregator metricsAggregator,
            TrendAnalyzer trendAnalyzer,
            AqiCalculator aqiCalculator
    ) {
        this.externalDeviceService = externalDeviceService;
        this.alertingContextFacade = alertingContextFacade;
        this.liveMetricsStore = liveMetricsStore;
        this.snapshotRepository = snapshotRepository;
        this.metricsAggregator = metricsAggregator;
        this.trendAnalyzer = trendAnalyzer;
        this.aqiCalculator = aqiCalculator;
    }

    @Override
    @Transactional(readOnly = true)
    public OverviewDashboardSnapshot handle(GetOverviewDashboardQuery query) {
        UUID ownerUserId = query.ownerUserId();
        int deviceLimit = query.deviceLimitPerSpace();
        int alertLimit = query.alertLimit();

        List<OrganizationSummary> orgs = externalDeviceService.findOrganizationsByOwnerId(ownerUserId);
        
        Set<UUID> allDeviceIds = new LinkedHashSet<>();
        List<OverviewDashboardSnapshot.OrganizationBreakdown> orgBreakdown = new ArrayList<>();
        for (OrganizationSummary org : orgs) {
            List<OverviewDashboardSnapshot.SpaceBreakdown> spaces = new ArrayList<>();
            for (SpaceSummary space : externalDeviceService.findSpacesByOrganizationId(org.organizationId())) {
                List<UUID> deviceIds = externalDeviceService.findDeviceIdsBySpaceId(space.spaceId(), deviceLimit);
                allDeviceIds.addAll(deviceIds);
                var metrics = aggregateAcrossDevices(deviceIds);
                spaces.add(new OverviewDashboardSnapshot.SpaceBreakdown(space.spaceId(), org.organizationId(),
                        space.spaceName(), metrics.aqiValue(), metrics.aqiCategory(), metrics.recordedAt(),
                        deviceIds.size(), metrics.freshness().name()));
            }
            orgBreakdown.add(new OverviewDashboardSnapshot.OrganizationBreakdown(
                    org.organizationId(), org.organizationName(), spaces));
        }
        int spaceCount = orgBreakdown.stream().mapToInt(ob -> ob.spaces().size()).sum();
        var recentAlerts = alertingContextFacade.getRecentAlertsByOwnerId(ownerUserId, DEFAULT_ALERT_STATUSES, alertLimit);
        List<OverviewDashboardSnapshot.AlertSummary> alertSummaries = enrichAlertNames(toAlertSummaries(recentAlerts));

        var overall = aggregateAcrossDevices(allDeviceIds.stream().toList());
        Instant updatedAt = overall.recordedAt() != null ? overall.recordedAt() : Instant.now();

        var core = new OverviewDashboardSnapshot.OverviewCoreMetrics(
                overall.aqiValue(),
                overall.aqiCategory(),
                overall.averageCo2(),
                overall.averagePm2_5(),
                overall.averageTemperature(),
                overall.averageHumidity(),
                overall.co2DeltaPercentage(),
                overall.pm2_5DeltaPercentage(),
                overall.temperatureDeltaPercentage(),
                overall.humidityDeltaPercentage(),
                overall.recordedAt(),
                orgs.size(),
                spaceCount,
                allDeviceIds.size(),
                overall.freshness().name()
        );

        return new OverviewDashboardSnapshot(core, orgBreakdown, alertSummaries, updatedAt);
    }

    private AggregatedMetrics aggregateAcrossDevices(List<UUID> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return new AggregatedMetrics(null, null, null, null, null, null, null, null, null, null, null, Freshness.NO_DATA);
        }

        // Dividir entre dispositivos en caché (LIVE) y los que necesitan Snapshot de la DB
        List<UUID> snapshotDeviceIds = new ArrayList<>();
        Map<UUID, DeviceMetricsSnapshot> cachedSnapshots = new HashMap<>();

        for (UUID deviceId : deviceIds) {
            var live = liveMetricsStore.getIfPresent(deviceId);
            var window = live == null ? Optional.<KpiLiveMetricsBuffer.Averages>empty() : live.computeAverages();
            if (window.isPresent()) {
                cachedSnapshots.put(deviceId, resolveLiveMetrics(window.orElseThrow()));
            } else {
                snapshotDeviceIds.add(deviceId);
            }
        }

        // Carga por lotes para los que no están en caché
        List<DeviceMetricsSnapshot> allSnapshots = new ArrayList<>(cachedSnapshots.values());
        if (!snapshotDeviceIds.isEmpty()) {
            var latestSnapshots = snapshotRepository.findLatestByDeviceIds(snapshotDeviceIds);
            for (var snapshot : latestSnapshots) {
                allSnapshots.add(new DeviceMetricsSnapshot(
                        DeviceMetricsSnapshot.Source.SNAPSHOT,
                        snapshot.getCalculatedAqi().value(),
                        snapshot.getAverageCo2(),
                        snapshot.getAveragePm2_5(),
                        snapshot.getAverageTemperature(),
                        snapshot.getAverageHumidity(),
                        null, // Delta simplificado para resumen batch
                        null,
                        null,
                        null,
                        snapshot.getTimeWindowEnd()
                ));
            }
        }

        return metricsAggregator.aggregate(allSnapshots);
    }

    private DeviceMetricsSnapshot resolveLiveMetrics(KpiLiveMetricsBuffer.Averages avg) {
        var aqi = aqiCalculator.calculateAqi(avg.pm2_5());

        return new DeviceMetricsSnapshot(
                DeviceMetricsSnapshot.Source.LIVE,
                aqi.value(),
                avg.co2(),
                avg.pm2_5(),
                avg.temperature(),
                avg.humidity(),
                null, null, null, null,
                avg.measuredAt()
        );
    }

    private static List<OverviewDashboardSnapshot.AlertSummary> toAlertSummaries(List<AlertDetails> alerts) {
        if (alerts == null || alerts.isEmpty()) return List.of();
        return alerts.stream().map(a -> new OverviewDashboardSnapshot.AlertSummary(
                a.alertId(),
                a.deviceId(),
                a.spaceId(),
                a.deviceName(),
                null,
                a.message(),
                a.severity(),
                a.status(),
                a.occurredAt()
        )).toList();
    }

    private List<OverviewDashboardSnapshot.AlertSummary> enrichAlertNames(List<OverviewDashboardSnapshot.AlertSummary> alerts) {
        if (alerts == null || alerts.isEmpty()) return List.of();

        List<UUID> deviceIds = alerts.stream()
                .map(OverviewDashboardSnapshot.AlertSummary::deviceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<UUID> spaceIds = alerts.stream()
                .map(OverviewDashboardSnapshot.AlertSummary::spaceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<UUID, String> deviceNames = externalDeviceService.findDeviceNamesByDeviceIds(deviceIds);
        Map<UUID, String> spaceNames = externalDeviceService.findSpaceNamesBySpaceIds(spaceIds);

        return alerts.stream().map(a -> new OverviewDashboardSnapshot.AlertSummary(
                a.alertId(),
                a.deviceId(),
                a.spaceId(),
                a.deviceName() != null ? a.deviceName() : deviceNames.get(a.deviceId()),
                a.spaceName() != null ? a.spaceName() : spaceNames.get(a.spaceId()),
                a.message(),
                a.severity(),
                a.status(),
                a.occurredAt()
        )).toList();
    }
}
