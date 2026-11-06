package com.claircore.billing.interfaces.acl;

import java.util.UUID;

public interface BillingContextFacade {

    int getMaxOrganizations(UUID userId);

    int getMaxSpaces(UUID userId);

    int getMaxDevices(UUID userId);

    boolean canAccessMonthlyReports(UUID userId);

    int getAssistantDailyMessageLimit(UUID userId);

    String getPlanName(UUID userId);
}
