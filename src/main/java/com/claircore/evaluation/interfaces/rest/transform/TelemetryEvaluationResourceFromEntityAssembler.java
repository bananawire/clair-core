package com.claircore.evaluation.interfaces.rest.transform;

import com.claircore.evaluation.domain.model.aggregates.TelemetryEvaluation;
import com.claircore.evaluation.interfaces.rest.resources.TelemetryEvaluationResource;

public final class TelemetryEvaluationResourceFromEntityAssembler {

    private TelemetryEvaluationResourceFromEntityAssembler() {
    }

    public static TelemetryEvaluationResource toResourceFromEntity(TelemetryEvaluation e) {
        var aq = e.getAirQuality();
        var pm = e.getParticulateMatter();
        var conn = e.getConnectivity();
        var loc = e.getLocation();

        return new TelemetryEvaluationResource(
                e.getId(),
                e.getDeviceId().value(),
                e.getReadingId(),
                e.getUptime(),
                new TelemetryEvaluationResource.AirQualityResource(
                        aq.co2(), aq.temperature(), aq.humidity()
                ),
                new TelemetryEvaluationResource.ParticulateMatterResource(
                        pm.pm1_0(), pm.pm2_5(), pm.pm10()
                ),
                new TelemetryEvaluationResource.ConnectivityResource(
                        conn.status(),
                        conn.network(),
                        conn.signalStrength()
                ),
                new TelemetryEvaluationResource.LocationResource(
                        loc.country()
                ),
                e.getHealthStatus(),
                e.getStatus(),
                e.getRecordedAt(),
                e.getCreatedAt()
        );
    }
}
