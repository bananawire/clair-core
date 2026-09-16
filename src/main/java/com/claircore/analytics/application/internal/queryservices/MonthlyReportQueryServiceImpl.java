package com.claircore.analytics.application.internal.queryservices;

import com.claircore.analytics.application.queryservices.MonthlyReportQueryService;
import com.claircore.analytics.domain.model.aggregates.DeviceMonthlySummary;
import com.claircore.analytics.domain.model.queries.GetMonthlyReportQuery;
import com.claircore.analytics.domain.repositories.DeviceMonthlySummaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class MonthlyReportQueryServiceImpl implements MonthlyReportQueryService {

    private final DeviceMonthlySummaryRepository monthlySummaryRepository;

    public MonthlyReportQueryServiceImpl(DeviceMonthlySummaryRepository monthlySummaryRepository) {
        this.monthlySummaryRepository = monthlySummaryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeviceMonthlySummary> handle(GetMonthlyReportQuery query) {
        return monthlySummaryRepository.findByDeviceIdAndMonth(
                query.deviceId().value(), query.month().withDayOfMonth(1));
    }
}
