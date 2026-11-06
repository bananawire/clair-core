package com.claircore.assistant.application.internal.outboundservices.acl;

import com.claircore.alerting.interfaces.acl.AlertDetails;
import com.claircore.alerting.interfaces.acl.AlertingContextFacade;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/** Consumer-side ACL for the user's open alerts, owned by the Alerting bounded context. */
@Service("assistantExternalAlertingService")
public class ExternalAlertingService {

    private static final List<String> ACTIVE = List.of("ACTIVE");

    private final AlertingContextFacade alertingContextFacade;

    public ExternalAlertingService(AlertingContextFacade alertingContextFacade) {
        this.alertingContextFacade = alertingContextFacade;
    }

    public List<AlertDetails> findActiveAlertsByOwnerId(UUID ownerUserId, int limit) {
        return alertingContextFacade.getRecentAlertsByOwnerId(ownerUserId, ACTIVE, limit);
    }
}
