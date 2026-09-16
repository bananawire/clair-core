package com.claircore.localedge.application.internal.outboundservices.acl;

import com.claircore.device.interfaces.acl.DeviceContextFacade;
import com.claircore.device.interfaces.acl.DeviceTelemetryTarget;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Consumer-side ACL facade wrapping the device bounded context. LocalEdge uses it through this
 * narrow surface instead of touching {@code DeviceRepository} or {@code DeviceAssignmentRepository}
 * directly, which keeps the BC isolation enforced.
 */
@org.springframework.stereotype.Service
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
    public List<DeviceTelemetryTarget> findTelemetryTargets(int limit, boolean includeUnassigned) {
        return deviceContextFacade.findTelemetryTargets(limit, includeUnassigned);
    }

    /** Marks the device as ONLINE at the given instant. */
    public void recordPresence(UUID deviceId, String status, java.time.Instant occurredAt) {
        deviceContextFacade.recordDevicePresence(deviceId, status, occurredAt);
    }

    public Optional<UUID> findDeviceIdByHardwareId(String hardwareId) {
        return deviceContextFacade.findDeviceIdByHardwareId(hardwareId);
    }
}
