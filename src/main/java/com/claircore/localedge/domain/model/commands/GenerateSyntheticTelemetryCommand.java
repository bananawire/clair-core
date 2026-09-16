package com.claircore.localedge.domain.model.commands;

import com.claircore.localedge.domain.model.valueobjects.SimulationScenario;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Inbound command that asks the LocalEdge bounded context to manufacture one cycle of telemetry
 * readings for the given devices and dispatch them through the ACL.
 *
 * @param deviceIds          devices the LocalEdge should service on this cycle
 * @param scenario           severity-band mix to apply
 * @param seed               deterministic seed; {@code 0L} for non-deterministic SecureRandom seeding
 * @param startedAt          wall-clock instant the scheduler issued the command
 */
public record GenerateSyntheticTelemetryCommand(
        List<UUID> deviceIds,
        SimulationScenario scenario,
        long seed,
        Instant startedAt
) {
    public GenerateSyntheticTelemetryCommand {
        if (deviceIds == null) {
            throw new IllegalArgumentException("deviceIds must not be null");
        }
        if (deviceIds.isEmpty()) {
            throw new IllegalArgumentException("deviceIds must not be empty");
        }
        if (scenario == null) {
            throw new IllegalArgumentException("scenario must not be null");
        }
        if (startedAt == null) {
            throw new IllegalArgumentException("startedAt must not be null");
        }
        deviceIds = List.copyOf(deviceIds);
    }
}
