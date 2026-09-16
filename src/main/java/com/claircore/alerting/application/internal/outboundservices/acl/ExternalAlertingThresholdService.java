package com.claircore.alerting.application.internal.outboundservices.acl;

import com.claircore.device.interfaces.acl.ThresholdContextFacade;
import com.claircore.device.interfaces.acl.ThresholdSummary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ExternalAlertingThresholdService {

    private final ThresholdContextFacade thresholdContextFacade;

    public ExternalAlertingThresholdService(ThresholdContextFacade thresholdContextFacade) {
        this.thresholdContextFacade = thresholdContextFacade;
    }

    public List<ThresholdSummary> fetchEnabledThresholdsByDeviceId(UUID deviceId) {
        return thresholdContextFacade.findEnabledThresholdsByDeviceId(deviceId);
    }
}
