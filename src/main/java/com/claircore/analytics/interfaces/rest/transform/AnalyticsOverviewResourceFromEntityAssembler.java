package com.claircore.analytics.interfaces.rest.transform;

import com.claircore.analytics.domain.model.valueobjects.OverviewDashboardSnapshot;
import com.claircore.analytics.interfaces.rest.resources.AnalyticsOverviewResponse;

public class AnalyticsOverviewResourceFromEntityAssembler {

    private AnalyticsOverviewResourceFromEntityAssembler() {}

    public static AnalyticsOverviewResponse toResponse(OverviewDashboardSnapshot snapshot) {
        var core = snapshot.core();
        var coreResource = new AnalyticsOverviewResponse.CoreMetrics(
                core.aqiValue(),
                core.aqiCategory(),
                core.averageCo2(),
                core.averagePm2_5(),
                core.averageTemperature(),
                core.averageHumidity(),
                core.co2DeltaPercentage(),
                core.pm2_5DeltaPercentage(),
                core.temperatureDeltaPercentage(),
                core.humidityDeltaPercentage(),
                core.recordedAt(),
                core.organizationCount(),
                core.spaceCount(),
                core.deviceCount(),
                core.dataFreshness()
        );

        var organizations = snapshot.organizations().stream()
                .map(o -> new AnalyticsOverviewResponse.OrganizationItem(
                        o.organizationId(),
                        o.organizationName(),
                        o.spaces().stream()
                                .map(s -> new AnalyticsOverviewResponse.SpaceItem(
                                        s.spaceId(),
                                        s.organizationId(),
                                        s.spaceName(),
                                        s.aqiValue(),
                                        s.aqiCategory(),
                                        s.recordedAt(),
                                        s.deviceCount(),
                                        s.dataFreshness()
                                ))
                                .toList()
                ))
                .toList();

        var alerts = snapshot.alerts().stream()
                .map(a -> new AnalyticsOverviewResponse.AlertItem(
                        a.alertId(),
                        a.deviceId(),
                        a.spaceId(),
                        a.deviceName(),
                        a.spaceName(),
                        a.message(),
                        a.severity(),
                        a.status(),
                        a.occurredAt()
                ))
                .toList();

        return new AnalyticsOverviewResponse(coreResource, organizations, alerts, snapshot.updatedAt());
    }
}
