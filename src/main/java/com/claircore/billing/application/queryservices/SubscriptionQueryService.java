package com.claircore.billing.application.queryservices;

import com.claircore.billing.domain.model.aggregates.PaymentRecord;
import com.claircore.billing.domain.model.queries.GetSubscriptionByIdQuery;
import com.claircore.billing.domain.model.queries.GetSubscriptionsByUserIdQuery;
import com.claircore.billing.domain.model.queries.GetUserPlanQuery;

import java.util.List;
import java.util.Optional;

/** Inbound port for the billing read side. */
public interface SubscriptionQueryService {
    com.claircore.billing.domain.model.valueobjects.PlanType resolveEffectivePlan(GetUserPlanQuery query);
    Optional<PaymentRecord> handle(GetSubscriptionByIdQuery query);
    List<PaymentRecord> handle(GetSubscriptionsByUserIdQuery query);
    /** Returns {@code "premium"} or {@code "freemium"}. */
    String resolveUserPlan(GetUserPlanQuery query);
}
