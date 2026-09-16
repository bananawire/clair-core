package com.claircore.analytics.application.queryservices;

import com.claircore.analytics.domain.model.aggregates.DeviceDailySummary;
import com.claircore.analytics.domain.model.queries.GetDailyReportQuery;

import java.util.Optional;

public interface DailyReportQueryService {

    Optional<DeviceDailySummary> handle(GetDailyReportQuery query);
}
