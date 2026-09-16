package com.claircore.evaluation.application.internal.outboundservices.acl;

import com.claircore.device.interfaces.acl.DeviceContextFacade;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Consumer-side ACL for resolving Device identifiers from the Device bounded context.
 *
 * The Evaluation bounded context treats the Device bounded context as the source of truth
 * for device identity. This service prevents direct repository coupling across contexts.
 */
@Service
public class ExternalDeviceService {

    private final DeviceContextFacade deviceContextFacade;

    public ExternalDeviceService(DeviceContextFacade deviceContextFacade) {
        this.deviceContextFacade = deviceContextFacade;
    }

    public Optional<UUID> findDeviceIdByHardwareId(String hardwareId) {
        return deviceContextFacade.findDeviceIdByHardwareId(hardwareId);
    }

    public Optional<String> findHardwareIdByDeviceId(UUID deviceId) {
        return deviceContextFacade.findHardwareIdByDeviceId(deviceId);
    }

    public boolean isDeviceOwnedByUser(UUID deviceId, UUID userId) {
        return deviceContextFacade.isDeviceOwnedByUser(deviceId, userId);
    }

    public Optional<java.time.Instant> findVisibleSinceByDeviceId(UUID deviceId) {
        return deviceContextFacade.findVisibleSinceByDeviceId(deviceId);
    }
}
