package com.claircore.analytics.application.queryservices;

import com.claircore.analytics.domain.model.queries.GetHistoricalTrendQuery;
import com.claircore.analytics.domain.model.valueobjects.KpiTrendPoint;

import java.util.List;

public interface KpiHistoricalTrendQueryService {

    List<KpiTrendPoint> handle(GetHistoricalTrendQuery query);
}
