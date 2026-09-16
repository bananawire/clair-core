package com.claircore.analytics.application.queryservices;

import com.claircore.analytics.domain.model.queries.GetDashboardMetricsQuery;
import com.claircore.analytics.domain.model.valueobjects.KpiDashboardMetrics;

import java.util.Optional;

public interface KpiDashboardMetricsQueryService {

    Optional<KpiDashboardMetrics> handle(GetDashboardMetricsQuery query);
}
