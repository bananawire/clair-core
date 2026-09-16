package com.claircore.analytics.domain.model.queries;

import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.domain.model.valueobjects.TrendPeriod;

import java.time.Instant;

/**
 * Request for a device's dashboard metrics. An explicit window wins; otherwise {@code period}
 * chooses one, and a null period means {@link TrendPeriod#LIVE}.
 */
public record GetDashboardMetricsQuery(
        DeviceId deviceId,
        TrendPeriod period,
        Instant startDate,
        Instant endDate
) {
    public GetDashboardMetricsQuery {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (startDate != null && endDate != null && !endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("endDate must be after startDate");
        }
    }
}
