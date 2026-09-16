package com.claircore.localedge.application.internal.commandservices;

import com.claircore.localedge.domain.model.commands.GenerateSyntheticTelemetryCommand;

/**
 * Application service that orchestrates one LocalEdge cycle: it asks the device facade for an
 * iteration of targets, asks the evaluation BC to record the manufactured readings, and asks the
 * device BC to keep the {@code DeviceAssignment} presence timestamp current.
 *
 * <p>Each ACL call is its own transaction (the receiving command services own their @Transactional
 * boundary); the LocalEdge itself holds no persistent state.
 */
public interface LocalEdgeTelemetryCommandService {

    /**
     * Runs one synthetic cycle: dispatches {@code GenerateSyntheticTelemetryCommand} and returns the
     * number of readings actually recorded by the Evaluation BC.
     *
     * @param command  inbound cycle instruction
     * @return number of accepted readings; failures and rejections are logged at INFO and skipped
     */
    int handle(GenerateSyntheticTelemetryCommand command);

    /**
     * Outcome detail so the scheduler can log INFO with the right numbers without re-querying.
     */
    record CycleOutcome(int accepted, int rejected, int presenceUpdates) {}
}
