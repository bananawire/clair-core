package com.claircore.localedge.application.internal.outboundservices.acl;

import com.claircore.device.interfaces.acl.DeviceContextFacade;
import com.claircore.device.interfaces.acl.DeviceTelemetryTarget;
import com.claircore.device.interfaces.acl.DeviceCommandForEdge;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Consumer-side ACL facade wrapping the device bounded context. LocalEdge uses it through this
 * narrow surface instead of touching device persistence directly, which keeps the BC isolation
 * enforced.
 */
@org.springframework.stereotype.Service("localedgeExternalDeviceService")
@ConditionalOnProperty(name = "claircore.local-edge.enabled", havingValue = "true", matchIfMissing = false)
public class ExternalDeviceService {

    private final DeviceContextFacade deviceContextFacade;

    public ExternalDeviceService(DeviceContextFacade deviceContextFacade) {
        this.deviceContextFacade = deviceContextFacade;
    }

    /**
     * Asks the device BC for the next page of telemetry targets. The LocalEdge asks for unassigned
     * units too so freshly-paired devices start producing readings immediately; the Evaluation BC
     * drops readings whose device is not in the device inventory, which would never happen here
     * because we asked the device BC for the list.
     */
    public List<DeviceTelemetryTarget> findTelemetryTargets(int limit, boolean includeDeleted) {
        return deviceContextFacade.findTelemetryTargets(limit, includeDeleted);
    }

    /** Marks the device as ONLINE at the given instant. */
    public void recordPresence(UUID deviceId, String status, java.time.Instant occurredAt) {
        deviceContextFacade.recordDevicePresence(deviceId, status, occurredAt);
    }

    public Optional<UUID> findDeviceIdByHardwareId(String hardwareId) {
        return deviceContextFacade.findDeviceIdByHardwareId(hardwareId);
    }

    public List<DeviceCommandForEdge> findClaimableCommands(Instant leaseCutoff, int limit) {
        return deviceContextFacade.findClaimableCommands(leaseCutoff, limit);
    }

    public Optional<DeviceCommandForEdge> claimCommand(UUID commandId, Instant leaseCutoff, Instant claimedAt) {
        return deviceContextFacade.claimCommand(commandId, leaseCutoff, claimedAt);
    }

    public void acknowledgeCommand(DeviceCommandForEdge command, boolean success, String reason) {
        deviceContextFacade.acknowledgeCommand(command.deviceId(), command.commandId(),
                success ? "EXECUTED" : "FAILED", reason);
    }
}
