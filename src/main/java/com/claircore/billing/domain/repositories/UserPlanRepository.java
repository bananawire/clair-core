package com.claircore.billing.domain.repositories;

import com.claircore.billing.domain.model.aggregates.UserPlan;
import com.claircore.billing.domain.model.valueobjects.UserId;

import java.util.Optional;

/** Port for user plan storage. Domain types only. */
public interface UserPlanRepository {

    UserPlan save(UserPlan userPlan);

    Optional<UserPlan> findByUserId(UserId userId);
}
