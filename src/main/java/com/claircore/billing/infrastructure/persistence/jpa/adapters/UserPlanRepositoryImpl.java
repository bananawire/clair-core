package com.claircore.billing.infrastructure.persistence.jpa.adapters;

import com.claircore.billing.domain.model.aggregates.UserPlan;
import com.claircore.billing.domain.model.valueobjects.UserId;
import com.claircore.billing.domain.repositories.UserPlanRepository;
import com.claircore.billing.infrastructure.persistence.jpa.assemblers.UserPlanPersistenceAssembler;
import com.claircore.billing.infrastructure.persistence.jpa.repositories.UserPlanPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserPlanRepositoryImpl implements UserPlanRepository {

    private final UserPlanPersistenceRepository userPlanPersistenceRepository;

    public UserPlanRepositoryImpl(UserPlanPersistenceRepository userPlanPersistenceRepository) {
        this.userPlanPersistenceRepository = userPlanPersistenceRepository;
    }

    @Override
    public UserPlan save(UserPlan userPlan) {
        var saved = userPlanPersistenceRepository.save(
                UserPlanPersistenceAssembler.toPersistenceFromDomain(userPlan));
        return UserPlanPersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public Optional<UserPlan> findByUserId(UserId userId) {
        return userPlanPersistenceRepository.findByUserId(userId)
                .map(UserPlanPersistenceAssembler::toDomainFromPersistence);
    }
}
