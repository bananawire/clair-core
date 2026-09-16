package com.claircore.evaluation.interfaces.events;

import com.claircore.evaluation.domain.model.aggregates.TelemetryEvaluation;

import java.time.Instant;
import java.util.UUID;

/**
 * The published contract for a recorded telemetry reading: the only evaluation event another
 * context may listen to.
 *
 * <p>Alerting and analytics both listen to it; there is no internal telemetry event any more.
 */
public record TelemetryRecordedIntegrationEvent(
        UUID deviceId,
        double co2,
        double temperature,
        double humidity,
        double pm1_0,
        double pm2_5,
        double pm10,
        String connectivityStatus,
        String network,
        Integer signalStrength,
        String country,
        int healthStatus,
        String status,
        long uptimeSeconds,
        String readingId,
        Instant recordedAt
) {
    public static TelemetryRecordedIntegrationEvent from(TelemetryEvaluation evaluation) {
        return new TelemetryRecordedIntegrationEvent(
                evaluation.getDeviceId().value(),
                evaluation.getAirQuality().co2(),
                evaluation.getAirQuality().temperature(),
                evaluation.getAirQuality().humidity(),
                evaluation.getParticulateMatter().pm1_0(),
                evaluation.getParticulateMatter().pm2_5(),
                evaluation.getParticulateMatter().pm10(),
                evaluation.getConnectivity().status(),
                evaluation.getConnectivity().network(),
                evaluation.getConnectivity().signalStrength(),
                evaluation.getLocation().country(),
                evaluation.getHealthStatus(),
                evaluation.getStatus(),
                evaluation.getUptime(),
                evaluation.getReadingId().toString(),
                evaluation.getRecordedAt());
    }
}
