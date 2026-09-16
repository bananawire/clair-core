package com.claircore.device.interfaces.acl;

import java.util.List;
import java.util.UUID;

/** Published threshold contract for other contexts. */
public interface ThresholdContextFacade {
    List<ThresholdSummary> findEnabledThresholdsByDeviceId(UUID deviceId);
}
