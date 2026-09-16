package com.claircore.evaluation.application.queryservices;

import com.claircore.evaluation.domain.model.aggregates.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.queries.GetEvaluationsByDeviceQuery;
import com.claircore.evaluation.domain.model.queries.GetDeviceReadingsQuery;
import com.claircore.evaluation.domain.model.queries.GetHourlyTelemetryAveragesQuery;
import com.claircore.evaluation.domain.model.queries.GetLatestEvaluationByDeviceQuery;
import com.claircore.evaluation.domain.model.valueobjects.DeviceReading;
import com.claircore.evaluation.domain.model.valueobjects.HourlyDeviceAverage;
import com.claircore.shared.domain.model.PageResult;

import java.util.List;
import java.util.Optional;

public interface TelemetryEvaluationQueryService {
    PageResult<TelemetryEvaluation> handle(GetEvaluationsByDeviceQuery query);

    Optional<TelemetryEvaluation> handle(GetLatestEvaluationByDeviceQuery query);

    List<HourlyDeviceAverage> handle(GetHourlyTelemetryAveragesQuery query);

    List<DeviceReading> handle(GetDeviceReadingsQuery query);
}
