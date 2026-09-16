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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Default implementation of {@link LocalEdgeTelemetryCommandService}.
 *
 * <p>One cycle:
 * <ol>
 *   <li>Fetch up to {@code limit} telemetry targets from the device BC.</li>
 *   <li>Generate one {@link EnvironmentalTelemetry} per device via {@link SyntheticTelemetryGeneratorPolicy}.</li>
 *   <li>Record each reading via the evaluation BC ACL.</li>
 *   <li>Record {@code ONLINE} presence for each device so the device BC's {@code presenceAt}
 *       timestamp stays current.</li>
 * </ol>
 *
 * <p>The LocalEdge transaction is required: each downstream ACL call commits in its own
 * transaction anyway, but this lets the cycle summary roll back without losing what already
 * reached the Evaluation BC.
 */
@Service
public class LocalEdgeTelemetryCommandServiceImpl implements LocalEdgeTelemetryCommandService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalEdgeTelemetryCommandServiceImpl.class);
    private static final String PRESENCE_ONLINE = "ONLINE";

    private final ExternalDeviceService externalDeviceService;
    private final ExternalEvaluationService externalEvaluationService;
    private final SyntheticTelemetryGeneratorPolicy generator;

    public LocalEdgeTelemetryCommandServiceImpl(
            ExternalDeviceService externalDeviceService,
            ExternalEvaluationService externalEvaluationService,
            SyntheticTelemetryGeneratorPolicy generator) {
        this.externalDeviceService = externalDeviceService;
        this.externalEvaluationService = externalEvaluationService;
        this.generator = generator;
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

        List<UUID> deviceIds = command.deviceIds();
        if (deviceIds.size() > targets.size()) {
            // Trim to the number of targets returned by the device BC so we never reference a
            // device the BC has not surfaced for this cycle.
            deviceIds = deviceIds.subList(0, targets.size());
        }

        Instant now = command.startedAt();
        List<EnvironmentalTelemetry> readings = new ArrayList<>(deviceIds.size());
        for (int i = 0; i < deviceIds.size(); i++) {
            // Reading generation must remain deterministic across cycles reruns at the same seed:
            // each reading uses a slot of the shared generator seeded with the device index.
            SyntheticTelemetryGeneratorPolicy oneSlot =
                    new SyntheticTelemetryGeneratorPolicy(deriveSeed(command.seed(), i));
            readings.add(oneSlot.generateOne(command.scenario(), now));
        }

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

        int presenceUpdates = 0;
        for (UUID deviceId : deviceIds) {
            try {
                externalDeviceService.recordPresence(deviceId, PRESENCE_ONLINE, now);
                presenceUpdates++;
            } catch (RuntimeException ex) {
                LOGGER.info("LocalEdge presence update for device {} failed: {}", deviceId, ex.getMessage());
            }
        }

        return new CycleOutcome(accepted, rejected, presenceUpdates);
    }

    private static long deriveSeed(long baseSeed, int index) {
        // Steady hash of (baseSeed, index) so a fixed baseSeed always yields the same per-device
        // sub-seed; with baseSeed=0 we hand back 0 so the generator keeps its non-deterministic
        // SecureRandom branch (the device index is irrelevant when the whole sequence is non-deterministic).
        if (baseSeed == 0L) return 0L;
        long h = baseSeed ^ ((long) index * 0x9E3779B97F4A7C15L);
        h ^= (h >>> 30) * 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 27) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return h;
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
