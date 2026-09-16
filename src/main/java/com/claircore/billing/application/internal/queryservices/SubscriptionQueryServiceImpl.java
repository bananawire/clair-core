package com.claircore.billing.application.internal.queryservices;

import com.claircore.billing.application.queryservices.SubscriptionQueryService;
import com.claircore.billing.domain.model.aggregates.PaymentRecord;
import com.claircore.billing.domain.model.queries.GetSubscriptionByIdQuery;
import com.claircore.billing.domain.model.queries.GetSubscriptionsByUserIdQuery;
import com.claircore.billing.domain.model.queries.GetUserPlanQuery;
import com.claircore.billing.domain.model.valueobjects.PlanType;
import com.claircore.billing.domain.model.valueobjects.UserId;
import com.claircore.billing.domain.repositories.PaymentRecordRepository;
import com.claircore.billing.domain.repositories.UserPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SubscriptionQueryServiceImpl implements SubscriptionQueryService {

    private final PaymentRecordRepository paymentRecordRepository;
    private final UserPlanRepository userPlanRepository;

    public SubscriptionQueryServiceImpl(PaymentRecordRepository paymentRecordRepository,
                                        UserPlanRepository userPlanRepository) {
        this.paymentRecordRepository = paymentRecordRepository;
        this.userPlanRepository = userPlanRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaymentRecord> handle(GetSubscriptionByIdQuery query) {
        return paymentRecordRepository.findById(query.subscriptionId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentRecord> handle(GetSubscriptionsByUserIdQuery query) {
        return paymentRecordRepository.findAllByUserId(query.userId());
    }

    @Override
    @Transactional(readOnly = true)
    public String resolveUserPlan(GetUserPlanQuery query) {
        return resolveEffectivePlan(query).name().toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    @Transactional(readOnly = true)
    public PlanType resolveEffectivePlan(GetUserPlanQuery query) {
        var uid = new UserId(UUID.fromString(query.userId()));
        return userPlanRepository.findByUserId(uid)
                .map(userPlan -> userPlan.isPremiumExpired() ? PlanType.FREEMIUM : userPlan.getPlanType())
                .orElse(PlanType.FREEMIUM);
    }
}
