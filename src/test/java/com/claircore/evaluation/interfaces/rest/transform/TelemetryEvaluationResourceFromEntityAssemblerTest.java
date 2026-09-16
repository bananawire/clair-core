package com.claircore.evaluation.interfaces.rest.transform;

import com.claircore.evaluation.domain.model.aggregates.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.valueobjects.*;
import com.claircore.evaluation.interfaces.rest.resources.TelemetryEvaluationResource;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class TelemetryEvaluationResourceFromEntityAssemblerTest {

    @Test
    void shouldTransformTelemetryEvaluationToResponseSuccessfully() {
        // Arrange
        UUID deviceId = UUID.randomUUID();
        TelemetryEvaluation evaluation = new TelemetryEvaluation(
                new DeviceId(deviceId),
                UUID.fromString("00000000-0000-0000-0000-000000000123"),
                3600L,
                new AirQuality(400.0, 22.0, 45.0),
                new ParticulateMatter(10.0, 15.0, 25.0),
                new Connectivity("ONLINE", "WiFi", -50),
                new Location("Chile"),
                85,
                "STABLE",
                Instant.now()
        );

        // Act
        TelemetryEvaluationResource response = TelemetryEvaluationResourceFromEntityAssembler.toResourceFromEntity(evaluation);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.deviceId()).isEqualTo(deviceId);
        assertThat(response.uptime()).isEqualTo(3600L);
        assertThat(response.airQuality().co2()).isEqualTo(400.0);
        assertThat(response.connectivity().status()).isEqualTo("ONLINE");
        assertThat(response.location().country()).isEqualTo("Chile");
    }
}
