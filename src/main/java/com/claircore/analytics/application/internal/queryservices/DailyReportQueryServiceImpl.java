package com.claircore.analytics.application.internal.queryservices;

import com.claircore.analytics.application.queryservices.DailyReportQueryService;
import com.claircore.analytics.domain.model.aggregates.DeviceDailySummary;
import com.claircore.analytics.domain.model.queries.GetDailyReportQuery;
import com.claircore.analytics.domain.repositories.DeviceDailySummaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class DailyReportQueryServiceImpl implements DailyReportQueryService {

    private final DeviceDailySummaryRepository dailySummaryRepository;

    public DailyReportQueryServiceImpl(DeviceDailySummaryRepository dailySummaryRepository) {
        this.dailySummaryRepository = dailySummaryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeviceDailySummary> handle(GetDailyReportQuery query) {
        if (query.date() != null) {
            return dailySummaryRepository.findByDeviceIdAndDate(query.deviceId().value(), query.date());
        }
        return dailySummaryRepository.findLatestByDeviceId(query.deviceId().value());
    }
}
