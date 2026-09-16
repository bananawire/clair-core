package com.claircore.analytics.application.internal.commandservices;

import com.claircore.analytics.application.commandservices.KpiLiveMetricsCommandService;
import com.claircore.analytics.application.internal.outboundservices.cache.LiveMetricsStore;
import com.claircore.analytics.domain.model.commands.ProcessTelemetryAnalyticCommand;
import com.claircore.analytics.domain.model.events.TelemetryReceivedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class KpiLiveMetricsCommandServiceImpl implements KpiLiveMetricsCommandService {

    private final LiveMetricsStore liveMetricsStore;
    private final ApplicationEventPublisher eventPublisher;

    public KpiLiveMetricsCommandServiceImpl(
            LiveMetricsStore liveMetricsStore,
            ApplicationEventPublisher eventPublisher
    ) {
        this.liveMetricsStore = liveMetricsStore;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void handle(ProcessTelemetryAnalyticCommand command) {
        var buffer = liveMetricsStore.getOrCreate(command.deviceId().value());
        buffer.add(
                command.recordedAt(),
                command.co2(),
                command.pm2_5(),
                command.temperature(),
                command.humidity()
        );

        eventPublisher.publishEvent(new TelemetryReceivedEvent(
                command.deviceId().value(),
                command.co2(),
                command.pm2_5(),
                command.temperature(),
                command.humidity(),
                command.recordedAt()
        ));
    }
}
