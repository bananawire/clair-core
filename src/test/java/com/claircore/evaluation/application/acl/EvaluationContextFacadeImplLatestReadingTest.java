package com.claircore.evaluation.application.acl;

import com.claircore.evaluation.application.commandservices.TelemetryEvaluationCommandService;
import com.claircore.evaluation.application.queryservices.TelemetryEvaluationQueryService;
import com.claircore.evaluation.domain.model.aggregates.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.queries.GetLatestEvaluationByDeviceQuery;
import com.claircore.evaluation.domain.model.valueobjects.AirQuality;
import com.claircore.evaluation.domain.model.valueobjects.Connectivity;
import com.claircore.evaluation.domain.model.valueobjects.DeviceId;
import com.claircore.evaluation.domain.model.valueobjects.Location;
import com.claircore.evaluation.domain.model.valueobjects.ParticulateMatter;
import com.claircore.evaluation.interfaces.acl.TelemetryReading;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationContextFacadeImplLatestReadingTest {

    @Mock TelemetryEvaluationQueryService queryService;
    @Mock TelemetryEvaluationCommandService commandService;

    @Test
    void getLatestReadingPublishesTheNewestEvaluationAsPrimitives() {
        UUID deviceId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Instant visibleSince = Instant.parse("2026-05-01T00:00:00Z");
        Instant recordedAt = Instant.parse("2026-05-16T22:30:00Z");
        UUID readingId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        TelemetryEvaluation latest = TelemetryEvaluation.reconstitute(
                readingId, new DeviceId(deviceId), readingId, 0L,
                new AirQuality(950.0, 24.5, 55.0),
                new ParticulateMatter(5.0, 18.0, 25.0),
                new Connectivity("ONLINE", "wifi", -50),
                new Location("Peru"),
                95, "OK", recordedAt, recordedAt, recordedAt);
        when(queryService.handle(new GetLatestEvaluationByDeviceQuery(deviceId, visibleSince)))
                .thenReturn(Optional.of(latest));

        var facade = new EvaluationContextFacadeImpl(queryService, commandService);
        Optional<TelemetryReading> reading = facade.getLatestReading(deviceId, visibleSince);

        assertThat(reading).contains(new TelemetryReading(deviceId, 950.0, 18.0, 24.5, 55.0, recordedAt));
    }

    @Test
    void getLatestReadingIsEmptyWhenTheDeviceHasNoVisibleReadings() {
        UUID deviceId = UUID.randomUUID();
        when(queryService.handle(new GetLatestEvaluationByDeviceQuery(deviceId, null)))
                .thenReturn(Optional.empty());

        var facade = new EvaluationContextFacadeImpl(queryService, commandService);

        assertThat(facade.getLatestReading(deviceId, null)).isEmpty();
    }
}
