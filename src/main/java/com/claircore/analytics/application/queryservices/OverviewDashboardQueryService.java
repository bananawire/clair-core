package com.claircore.analytics.application.queryservices;

import com.claircore.analytics.domain.model.queries.GetOverviewDashboardQuery;
import com.claircore.analytics.domain.model.valueobjects.OverviewDashboardSnapshot;

public interface OverviewDashboardQueryService {
    OverviewDashboardSnapshot handle(GetOverviewDashboardQuery query);
}

