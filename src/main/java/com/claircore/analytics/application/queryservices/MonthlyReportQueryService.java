package com.claircore.analytics.application.queryservices;

import com.claircore.analytics.domain.model.aggregates.DeviceMonthlySummary;
import com.claircore.analytics.domain.model.queries.GetMonthlyReportQuery;

import java.util.Optional;

public interface MonthlyReportQueryService {

    Optional<DeviceMonthlySummary> handle(GetMonthlyReportQuery query);
}
