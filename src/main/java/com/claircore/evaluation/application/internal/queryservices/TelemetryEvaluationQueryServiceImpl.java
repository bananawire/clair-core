package com.claircore.evaluation.application.internal.queryservices;

import com.claircore.evaluation.application.queryservices.TelemetryEvaluationQueryService;
import com.claircore.evaluation.domain.model.aggregates.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.queries.GetEvaluationsByDeviceQuery;
import com.claircore.evaluation.domain.model.queries.GetDeviceReadingsQuery;
import com.claircore.evaluation.domain.model.queries.GetHourlyTelemetryAveragesQuery;
import com.claircore.evaluation.domain.model.queries.GetLatestEvaluationByDeviceQuery;
import com.claircore.evaluation.domain.model.valueobjects.DeviceReading;
import com.claircore.evaluation.domain.model.valueobjects.HourlyDeviceAverage;
import com.claircore.evaluation.domain.repositories.TelemetryEvaluationRepository;
import com.claircore.shared.domain.model.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TelemetryEvaluationQueryServiceImpl implements TelemetryEvaluationQueryService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    private final TelemetryEvaluationRepository telemetryEvaluationRepository;

    public TelemetryEvaluationQueryServiceImpl(TelemetryEvaluationRepository telemetryEvaluationRepository) {
        this.telemetryEvaluationRepository = telemetryEvaluationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<TelemetryEvaluation> handle(GetEvaluationsByDeviceQuery query) {
        int page = query.page() != null ? query.page() : DEFAULT_PAGE;
        int size = query.size() != null ? query.size() : DEFAULT_SIZE;
        return query.visibleSince() == null
                ? telemetryEvaluationRepository.findByDeviceId(query.deviceId(), page, size)
                : telemetryEvaluationRepository.findByDeviceIdSince(query.deviceId(), query.visibleSince(), page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TelemetryEvaluation> handle(GetLatestEvaluationByDeviceQuery query) {
        return query.visibleSince() == null
                ? telemetryEvaluationRepository.findLatestByDeviceId(query.deviceId())
                : telemetryEvaluationRepository.findLatestByDeviceIdSince(query.deviceId(), query.visibleSince());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HourlyDeviceAverage> handle(GetHourlyTelemetryAveragesQuery query) {
        return telemetryEvaluationRepository.findHourlyAveragesBetween(query.start(), query.end());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceReading> handle(GetDeviceReadingsQuery query) {
        return telemetryEvaluationRepository.findReadingsBetween(query.start(), query.end());
    }
}
