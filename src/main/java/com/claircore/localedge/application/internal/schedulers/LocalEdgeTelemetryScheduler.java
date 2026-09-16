package com.claircore.localedge.application.internal.schedulers;

import com.claircore.device.interfaces.acl.DeviceTelemetryTarget;
import com.claircore.localedge.application.internal.commandservices.LocalEdgeTelemetryCommandService;
import com.claircore.localedge.application.internal.outboundservices.acl.ExternalDeviceService;
import com.claircore.localedge.domain.model.commands.GenerateSyntheticTelemetryCommand;
import com.claircore.localedge.domain.model.valueobjects.SimulationScenario;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Drives the LocalEdge bounded context on a timer. Reads the interval from
 * {@code claircore.local-edge.interval-ms} (default 15000 ms) and starts after
 * {@code claircore.local-edge.initial-delay-ms} (default 15000 ms). Enabled only when
 * {@code claircore.local-edge.enabled} is exactly {@code "true"}.
 *
 * <p>When {@code claircore.local-edge.target-limit} is 0 the whole cycle is skipped without
 * touching the {@code @ConditionalOnProperty} gate. Operators who want to leave the bean
 * registered but stop emitting traffic can flip a single integer.
 */
@Component
@ConditionalOnProperty(
        name = "claircore.local-edge.enabled",
        havingValue = "true",
        matchIfMissing = false)
public class LocalEdgeTelemetryScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalEdgeTelemetryScheduler.class);

    private final ExternalDeviceService externalDeviceService;
    private final LocalEdgeTelemetryCommandService commandService;
    private final SimulationScenario scenario;
    private final long seed;
    private final int targetLimit;
    private final AtomicLong cyclesSkipped = new AtomicLong();

    public LocalEdgeTelemetryScheduler(
            ExternalDeviceService externalDeviceService,
            LocalEdgeTelemetryCommandService commandService,
            @Value("${claircore.local-edge.scenario:MIXED}") String scenarioName,
            @Value("${claircore.local-edge.seed:0}") long seed,
            @Value("${claircore.local-edge.target-limit:50}") int targetLimit) {
        this.externalDeviceService = externalDeviceService;
        this.commandService = commandService;
        this.scenario = parseScenario(scenarioName);
        this.seed = seed;
        this.targetLimit = targetLimit;
    }

    @Scheduled(
            fixedDelayString = "${claircore.local-edge.interval-ms:15000}",
            initialDelayString = "${claircore.local-edge.initial-delay-ms:15000}")
    public void cycle() {
        if (targetLimit <= 0) {
            long skipped = cyclesSkipped.incrementAndGet();
            LOGGER.info("LocalEdge scheduler skipped (target-limit={}) skipped-cycles={}", targetLimit, skipped);
            return;
        }

        List<DeviceTelemetryTarget> targets =
                externalDeviceService.findTelemetryTargets(targetLimit, false);
        if (targets.isEmpty()) {
            LOGGER.info("LocalEdge scheduler observed no telemetry targets");
            return;
        }

        List<UUID> deviceIds = targets.stream()
                .filter(target -> !target.isStandby())
                .map(DeviceTelemetryTarget::deviceId)
                .distinct()
                .toList();
        if (deviceIds.isEmpty()) {
            LOGGER.info("LocalEdge scheduler found no active telemetry targets");
            return;
        }
        int recorded = commandService.handle(new GenerateSyntheticTelemetryCommand(
                deviceIds, scenario, seed, Instant.now()));
        LOGGER.info("LocalEdge scheduler emitted {} readings to evaluation BC (targets={})",
                recorded, deviceIds.size());
    }

    private static SimulationScenario parseScenario(String name) {
        try {
            return SimulationScenario.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return SimulationScenario.MIXED;
        }
    }

    int targetLimit() {
        return targetLimit;
    }
}
