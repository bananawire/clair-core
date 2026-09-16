package com.claircore.localedge.application.internal.schedulers;

import com.claircore.device.interfaces.acl.DeviceCommandForEdge;
import com.claircore.localedge.application.LocalDeviceCommandExecutor;
import com.claircore.localedge.application.internal.outboundservices.acl.ExternalDeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/** Claims and executes device commands with at-least-once delivery and lease recovery. */
@Component
@ConditionalOnProperty(name = "claircore.local-edge.enabled", havingValue = "true")
public class LocalEdgeCommandScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalEdgeCommandScheduler.class);
    private static final long MAX_LEASE_SECONDS = 86_400L;

    private final ExternalDeviceService devices;
    private final LocalDeviceCommandExecutor executor;
    private final int batchSize;
    private final long leaseSeconds;

    public LocalEdgeCommandScheduler(ExternalDeviceService devices, LocalDeviceCommandExecutor executor,
                                     @Value("${claircore.local-edge.command-batch-size:25}") int batchSize,
                                     @Value("${claircore.local-edge.command-lease-seconds:60}") long leaseSeconds) {
        this.devices = devices;
        this.executor = executor;
        this.batchSize = Math.max(1, Math.min(batchSize, 500));
        this.leaseSeconds = Math.min(Math.max(1, leaseSeconds), MAX_LEASE_SECONDS);
    }

    @Scheduled(fixedDelayString = "${claircore.local-edge.command-poll-ms:5000}")
    public void poll() {
        Instant now = Instant.now();
        Instant leaseCutoff = now.minusSeconds(leaseSeconds);
        List<DeviceCommandForEdge> candidates = devices.findClaimableCommands(leaseCutoff, batchSize);
        for (DeviceCommandForEdge candidate : candidates) {
            try {
                devices.claimCommand(candidate.commandId(), leaseCutoff, now)
                        .ifPresent(this::executeAndAcknowledge);
            } catch (RuntimeException ex) {
                // A failed claim is not an execution failure. Another poll can retry it and the
                // remaining candidates in this bounded batch should still be attempted.
                LOGGER.warn("LocalEdge could not claim command {}: {}",
                        candidate.commandId(), ex.getMessage());
            }
        }
    }

    private void executeAndAcknowledge(DeviceCommandForEdge command) {
        boolean executed = true;
        String failureReason = null;
        try {
            executor.execute(command);
        } catch (RuntimeException ex) {
            executed = false;
            failureReason = ex.getMessage();
        }

        try {
            devices.acknowledgeCommand(command, executed, failureReason);
        } catch (RuntimeException ex) {
            // Do not turn an ACK transport/persistence failure into a FAILED execution. The
            // command remains SENT and will be offered again after the lease expires.
            LOGGER.warn("LocalEdge could not acknowledge command {}: {}",
                    command.commandId(), ex.getMessage());
        }
    }
}
