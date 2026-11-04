package com.claircore.assistant.application.internal.outboundservices.acl;

import com.claircore.billing.interfaces.acl.BillingContextFacade;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Consumer-side ACL for the assistant's daily allowance, which the Billing bounded context derives
 * from the user's effective plan.
 */
@Service("assistantExternalBillingService")
public class ExternalBillingService {

    private final BillingContextFacade billingContextFacade;

    public ExternalBillingService(BillingContextFacade billingContextFacade) {
        this.billingContextFacade = billingContextFacade;
    }

    public int getAssistantDailyMessageLimit(UUID userId) {
        return billingContextFacade.getAssistantDailyMessageLimit(userId);
    }

    public String getPlanName(UUID userId) {
        return billingContextFacade.getPlanName(userId);
    }
}
