package com.claircore.billing.infrastructure.persistence.jpa.assemblers;

import com.claircore.billing.domain.model.aggregates.UserPlan;
import com.claircore.billing.infrastructure.persistence.jpa.entities.UserPlanPersistenceEntity;

public final class UserPlanPersistenceAssembler {

    private UserPlanPersistenceAssembler() {
    }

    public static UserPlan toDomainFromPersistence(UserPlanPersistenceEntity entity) {
        if (entity == null) return null;
        return UserPlan.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getPlanType(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static UserPlanPersistenceEntity toPersistenceFromDomain(UserPlan userPlan) {
        if (userPlan == null) return null;
        var entity = new UserPlanPersistenceEntity();
        entity.setId(userPlan.getId());
        entity.setUserId(userPlan.getUserId());
        entity.setPlanType(userPlan.getPlanType());
        entity.setStartDate(userPlan.getStartDate());
        entity.setEndDate(userPlan.getEndDate());
        // Null for a plan that has never been written; that is what marks the entity as new.
        entity.setCreatedAt(userPlan.getCreatedAt());
        entity.setUpdatedAt(userPlan.getUpdatedAt());
        return entity;
    }
}
