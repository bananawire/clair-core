package com.claircore.analytics.application.internal.outboundservices.acl;

import com.claircore.evaluation.interfaces.acl.EvaluationContextFacade;
import com.claircore.evaluation.interfaces.acl.HourlyTelemetryAverage;
import com.claircore.evaluation.interfaces.acl.TelemetryReading;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ExternalEvaluationService {

    private final EvaluationContextFacade evaluationContextFacade;

    public ExternalEvaluationService(EvaluationContextFacade evaluationContextFacade) {
        this.evaluationContextFacade = evaluationContextFacade;
    }

    public List<HourlyTelemetryAverage> fetchHourlyTelemetryAggregation(Instant start, Instant end) {
        return evaluationContextFacade.getHourlyTelemetryAggregation(start, end);
    }

    public List<TelemetryReading> fetchReadings(Instant start, Instant end) {
        return evaluationContextFacade.getReadingsBetween(start, end);
    }
}
