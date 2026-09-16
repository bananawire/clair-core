package com.claircore.device.application.acl;

import com.claircore.device.application.queryservices.DeviceThresholdQueryService;
import com.claircore.device.interfaces.acl.ThresholdContextFacade;
import com.claircore.device.interfaces.acl.ThresholdSummary;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class ThresholdContextFacadeImpl implements ThresholdContextFacade {
    private final DeviceThresholdQueryService queryService;

    public ThresholdContextFacadeImpl(DeviceThresholdQueryService queryService) {
        this.queryService = queryService;
    }

    @Override
    public List<ThresholdSummary> findEnabledThresholdsByDeviceId(UUID deviceId) {
        return queryService.findEnabledByDeviceId(deviceId).stream()
                .map(t -> new ThresholdSummary(t.metric().name(), t.value(), t.enabled()))
                .toList();
    }
}
