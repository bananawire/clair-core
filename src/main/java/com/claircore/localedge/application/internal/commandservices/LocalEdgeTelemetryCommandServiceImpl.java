package com.claircore.localedge.application.internal.commandservices;

import com.claircore.evaluation.interfaces.acl.TelemetryRecordingResult;
import com.claircore.evaluation.interfaces.acl.TelemetrySubmission;
import com.claircore.device.interfaces.acl.DeviceTelemetryTarget;
import com.claircore.localedge.application.internal.outboundservices.acl.ExternalDeviceService;
import com.claircore.localedge.application.internal.outboundservices.acl.ExternalEvaluationService;
import com.claircore.localedge.domain.model.commands.GenerateSyntheticTelemetryCommand;
import com.claircore.localedge.domain.model.valueobjects.EnvironmentalSeverity;
import com.claircore.localedge.domain.model.valueobjects.EnvironmentalTelemetry;
import com.claircore.localedge.domain.model.valueobjects.SimulationScenario;
import com.claircore.localedge.domain.services.SyntheticTelemetryGeneratorPolicy;
import com.claircore.localedge.application.LocalDeviceCommandExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Default implementation of {@link LocalEdgeTelemetryCommandService}.
 *
 * <p>One cycle:
 * <ol>
 *   <li>Fetch up to {@code limit} telemetry targets from the device BC.</li>
 *   <li>Generate one {@link EnvironmentalTelemetry} per device via {@link SyntheticTelemetryGeneratorPolicy}.</li>
 *   <li>Record each reading via the evaluation BC ACL.</li>
 *   <li>Record {@code ONLINE} presence for each assigned device so the device BC's
 *       {@code presenceAt} timestamp stays current. Inventory-only devices have no assignment
 *       row, so their telemetry is stored without a presence update.</li>
 * </ol>
 *
 * <p>The LocalEdge transaction is required: each downstream ACL call commits in its own
 * transaction anyway, but this lets the cycle summary roll back without losing what already
 * reached the Evaluation BC.
 */
public class LocalEdgeTelemetryCommandServiceImpl implements LocalEdgeTelemetryCommandService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalEdgeTelemetryCommandServiceImpl.class);
    private static final String PRESENCE_ONLINE = "ONLINE";

    private final ExternalDeviceService externalDeviceService;
    private final ExternalEvaluationService externalEvaluationService;
    private final SyntheticTelemetryGeneratorPolicy generator;
    private final LocalDeviceCommandExecutor commandExecutor;

    public LocalEdgeTelemetryCommandServiceImpl(ExternalDeviceService externalDeviceService,
            ExternalEvaluationService externalEvaluationService, SyntheticTelemetryGeneratorPolicy generator) {
        this(externalDeviceService, externalEvaluationService, generator, deviceId -> { });
    }

    public LocalEdgeTelemetryCommandServiceImpl(ExternalDeviceService externalDeviceService,
            ExternalEvaluationService externalEvaluationService, SyntheticTelemetryGeneratorPolicy generator,
            LocalDeviceCommandExecutor commandExecutor) {
        this.externalDeviceService = externalDeviceService;
        this.externalEvaluationService = externalEvaluationService;
        this.generator = generator;
        this.commandExecutor = commandExecutor;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int handle(GenerateSyntheticTelemetryCommand command) {
        CycleOutcome outcome = runCycle(command);
        LOGGER.info("LocalEdge cycle accepted={} rejected={} presenceUpdates={} scenario={} seed={}",
                outcome.accepted(), outcome.rejected(), outcome.presenceUpdates(),
                command.scenario(), command.seed());
        return outcome.accepted();
    }

    /** Internal entry point used directly by tests; does not start a new transaction. */
    public CycleOutcome runCycle(GenerateSyntheticTelemetryCommand command) {
        List<DeviceTelemetryTarget> targets =
                externalDeviceService.findTelemetryTargets(command.deviceIds().size(), false);
        if (targets.isEmpty()) {
            LOGGER.info("LocalEdge cycle found no telemetry targets");
            return new CycleOutcome(0, 0, 0);
        }

        Map<UUID, DeviceTelemetryTarget> targetsByDevice = new HashMap<>();
        Map<UUID, Boolean> assignedByDevice = new HashMap<>();
        for (DeviceTelemetryTarget target : targets) {
            targetsByDevice.putIfAbsent(target.deviceId(), target);
            assignedByDevice.putIfAbsent(target.deviceId(), target.assigned());
        }

        // The device ACL is authoritative for the roster and persisted STANDBY status. The
        // executor state closes the small window before the ACK has been reflected in that ACL.
        List<UUID> deviceIds = command.deviceIds().stream()
                .distinct()
                .filter(targetsByDevice::containsKey)
                .filter(id -> !targetsByDevice.get(id).isStandby())
                .filter(id -> !commandExecutor.isStandby(id))
                .toList();
        if (deviceIds.isEmpty()) {
            LOGGER.info("LocalEdge cycle found no active telemetry targets");
            return new CycleOutcome(0, 0, 0);
        }

        Instant now = command.startedAt();
        // The generator is a singleton bean: it carries per-deviceId state across cycles,
        // so the AR(1) persistence term sees real X_{t-1} values instead of being reset to
        // the seasonal mean every dispatch. That is what keeps consecutive readings smooth.
        List<EnvironmentalTelemetry> readings = generator.generate(deviceIds, command.scenario(), now);

        int accepted = 0;
        int rejected = 0;
        for (int i = 0; i < readings.size(); i++) {
            EnvironmentalTelemetry reading = readings.get(i);
            UUID deviceId = deviceIds.get(i);
            TelemetrySubmission submission = toSubmission(deviceId, reading);
            try {
                TelemetryRecordingResult result = externalEvaluationService.recordTelemetry(submission);
                if (result.accepted()) {
                    accepted++;
                } else {
                    rejected++;
                    LOGGER.info("LocalEdge reading for device {} was rejected by evaluation BC readingId={}",
                            deviceId, result.readingId());
                }
            } catch (RuntimeException ex) {
                rejected++;
                LOGGER.info("LocalEdge reading for device {} failed: {}", deviceId, ex.getMessage());
            }
        }

        // Inventory-only devices are valid telemetry sources but do not have an assignment row
        // whose presence timestamp can be updated. Skip them before calling the transactional
        // Device BC operation; otherwise its expected exception would mark this cycle rollback-only.
        int presenceUpdates = 0;
        for (UUID deviceId : deviceIds) {
            if (!assignedByDevice.getOrDefault(deviceId, false)) {
                LOGGER.debug("LocalEdge presence skipped for unassigned device {}", deviceId);
                continue;
            }
            try {
                externalDeviceService.recordPresence(deviceId, PRESENCE_ONLINE, now);
                presenceUpdates++;
            } catch (RuntimeException ex) {
                LOGGER.info("LocalEdge presence update for device {} failed: {}", deviceId, ex.getMessage());
            }
        }

        return new CycleOutcome(accepted, rejected, presenceUpdates);
    }

    private static TelemetrySubmission toSubmission(UUID deviceId, EnvironmentalTelemetry reading) {
        // LocalEdge always publishes "ONLINE" connectivity, so the gauge always returns 95/70/40
        // from RECOMMENDED/ALERT/HIGH; binding it on the metric side would couple the BC, so we
        // infer the band from the readings instead and pick the matching static gauge here.
        EnvironmentalSeverity severity = inferSeverity(reading);
        int health = SyntheticTelemetryGeneratorPolicy.healthFor(severity);
        return new TelemetrySubmission(
                deviceId,
                reading.co2(),
                reading.temperature(),
                reading.humidity(),
                reading.pm1_0(),
                reading.pm2_5(),
                reading.pm10(),
                health,
                "OK",
                reading.country(),
                reading.networkName(),
                "ONLINE",
                Integer.valueOf(-50),
                reading.recordedAt());
    }

    /**
     * Closest-fit band using the same numeric ranges the generator picked from. Keeps the gauge
     * monotonic with severity without letting the API expose the band enum.
     */
    private static EnvironmentalSeverity inferSeverity(EnvironmentalTelemetry reading) {
        if (reading.pm2_5() <= 25.0 && reading.co2() <= 800.0
                && reading.temperature() >= 22.0 && reading.temperature() <= 26.0
                && reading.humidity() >= 40.0 && reading.humidity() <= 60.0) {
            return EnvironmentalSeverity.RECOMMENDED;
        }
        if (reading.pm2_5() > 50.0 || reading.co2() > 1000.0
                || reading.temperature() < 18.0 || reading.temperature() > 28.0
                || reading.humidity() < 35.0 || reading.humidity() > 65.0) {
            return EnvironmentalSeverity.HIGH;
        }
        return EnvironmentalSeverity.ALERT;
    }
}
