package com.claircore.billing.domain.model.aggregates;

import com.claircore.billing.domain.model.valueobjects.PlanType;
import com.claircore.billing.domain.model.valueobjects.UserId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** The plan a user is entitled to, and for how long. */
public class UserPlan {

    /** Days a premium plan stays active from the moment it is paid for. */
    private static final int PREMIUM_DURATION_DAYS = 30;

    private final UUID id;
    private final UserId userId;
    private PlanType planType;
    private LocalDate startDate;
    private LocalDate endDate;
    private final Instant createdAt;
    private final Instant updatedAt;

    private UserPlan(UUID id, UserId userId, PlanType planType, LocalDate startDate, LocalDate endDate,
                     Instant createdAt, Instant updatedAt) {
        if (id == null) {
            throw new IllegalArgumentException("Id must not be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        this.id = id;
        this.userId = userId;
        this.planType = planType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UserPlan(UserId userId) {
        this(UUID.randomUUID(), userId, PlanType.FREEMIUM, LocalDate.now(), null, null, null);
    }

    /** Rebuilds a plan that already exists in storage, identity and audit timestamps included. */
    public static UserPlan reconstitute(UUID id, UserId userId, PlanType planType, LocalDate startDate,
                                        LocalDate endDate, Instant createdAt, Instant updatedAt) {
        return new UserPlan(id, userId, planType, startDate, endDate, createdAt, updatedAt);
    }

    public void upgradeToPremium() {
        this.planType = PlanType.PREMIUM;
        this.startDate = LocalDate.now();
        this.endDate = this.startDate.plusDays(PREMIUM_DURATION_DAYS);
    }

    public void downgradeToFreemium() {
        this.planType = PlanType.FREEMIUM;
        this.endDate = null;
    }

    public boolean isPremiumExpired() {
        return this.planType == PlanType.PREMIUM && this.endDate != null && LocalDate.now().isAfter(this.endDate);
    }

    public UUID getId() { return id; }
    public UserId getUserId() { return userId; }
    public PlanType getPlanType() { return planType; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }

    /** Null until the plan has been written; assigned by persistence auditing. */
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
