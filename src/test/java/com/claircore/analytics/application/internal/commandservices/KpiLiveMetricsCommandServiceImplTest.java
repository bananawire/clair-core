package com.claircore.analytics.application.internal.commandservices;

import com.claircore.analytics.application.internal.outboundservices.cache.KpiLiveMetricsBuffer;
import com.claircore.analytics.application.internal.outboundservices.cache.LiveMetricsStore;
import com.claircore.analytics.domain.model.commands.ProcessTelemetryAnalyticCommand;
import com.claircore.analytics.domain.model.events.TelemetryReceivedEvent;
import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KpiLiveMetricsCommandServiceImplTest {

    @Mock
    private LiveMetricsStore liveMetricsStore;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private KpiLiveMetricsCommandServiceImpl commandService;

    @Test
    void shouldUpdateBufferAndPublishEventWhenHandlingCommand() {
        var deviceUuid = UUID.randomUUID();
        var deviceId = new DeviceId(deviceUuid);
        var now = Instant.now();
        var command = new ProcessTelemetryAnalyticCommand(
                deviceId,
                550.0,
                12.5,
                23.0,
                48.0,
                now
        );

        var buffer = spy(new KpiLiveMetricsBuffer());
        when(liveMetricsStore.getOrCreate(deviceUuid)).thenReturn(buffer);

        commandService.handle(command);

        // Verify buffer addition
        verify(buffer).add(now, 550.0, 12.5, 23.0, 48.0);

        // Verify event publication
        verify(eventPublisher).publishEvent(any(TelemetryReceivedEvent.class));
    }
}
