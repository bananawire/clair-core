package com.claircore.billing.application.acl;

import com.claircore.billing.domain.model.valueobjects.PlanType;
import com.claircore.billing.interfaces.acl.BillingContextFacade;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BillingContextFacadeImpl implements BillingContextFacade {

    private final com.claircore.billing.application.queryservices.SubscriptionQueryService queryService;

    public BillingContextFacadeImpl(com.claircore.billing.application.queryservices.SubscriptionQueryService queryService) {
        this.queryService = queryService;
    }

    private PlanType resolveEffectivePlanType(UUID userId) {
        return queryService.resolveEffectivePlan(new com.claircore.billing.domain.model.queries.GetUserPlanQuery(userId.toString()));
    }

    @Override
    public int getMaxOrganizations(UUID userId) {
        return resolveEffectivePlanType(userId).maxOrganizations();
    }

    @Override
    public int getMaxSpaces(UUID userId) {
        return resolveEffectivePlanType(userId).maxSpaces();
    }

    @Override
    public int getMaxDevices(UUID userId) {
        return resolveEffectivePlanType(userId).maxDevices();
    }

    @Override
    public boolean canAccessMonthlyReports(UUID userId) {
        return resolveEffectivePlanType(userId).monthlyReports();
    }
}
