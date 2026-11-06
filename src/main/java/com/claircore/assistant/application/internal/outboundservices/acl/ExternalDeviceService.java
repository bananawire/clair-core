package com.claircore.assistant.application.internal.outboundservices.acl;

import com.claircore.device.interfaces.acl.DeviceContextFacade;
import com.claircore.device.interfaces.acl.ThresholdContextFacade;
import com.claircore.device.interfaces.acl.ThresholdSummary;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Consumer-side ACL for the devices, spaces and thresholds the assistant describes to the model.
 * Both facades are published by the Device bounded context.
 */
@Service("assistantExternalDeviceService")
public class ExternalDeviceService {

    private final DeviceContextFacade deviceContextFacade;
    private final ThresholdContextFacade thresholdContextFacade;

    public ExternalDeviceService(DeviceContextFacade deviceContextFacade, ThresholdContextFacade thresholdContextFacade) {
        this.deviceContextFacade = deviceContextFacade;
        this.thresholdContextFacade = thresholdContextFacade;
    }

    public List<UUID> findDeviceIdsByOwnerId(UUID ownerUserId) {
        return deviceContextFacade.findDeviceIdsByOwnerId(ownerUserId);
    }

    public Map<UUID, String> findDeviceNamesByDeviceIds(List<UUID> deviceIds) {
        return deviceContextFacade.findDeviceNamesByDeviceIds(deviceIds);
    }

    public Optional<UUID> findSpaceIdByDeviceId(UUID deviceId) {
        return deviceContextFacade.findSpaceIdByDeviceId(deviceId);
    }

    public Map<UUID, String> findSpaceNamesBySpaceIds(List<UUID> spaceIds) {
        return deviceContextFacade.findSpaceNamesBySpaceIds(spaceIds);
    }

    /** Readings recorded before this instant belong to a previous owner and must not be shown. */
    public Optional<Instant> findVisibleSinceByDeviceId(UUID deviceId) {
        return deviceContextFacade.findVisibleSinceByDeviceId(deviceId);
    }

    public List<ThresholdSummary> findEnabledThresholdsByDeviceId(UUID deviceId) {
        return thresholdContextFacade.findEnabledThresholdsByDeviceId(deviceId);
    }
}
