package com.claircore.billing.application.internal.commandservices;

import com.claircore.billing.application.commandservices.UserPlanCommandService;
import com.claircore.billing.domain.model.commands.InitializeUserPlanCommand;
import com.claircore.billing.domain.model.aggregates.UserPlan;
import com.claircore.billing.domain.repositories.UserPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
public class UserPlanCommandServiceImpl implements UserPlanCommandService {
    private final UserPlanRepository repository;
    public UserPlanCommandServiceImpl(UserPlanRepository repository) { this.repository = repository; }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(InitializeUserPlanCommand command) {
        if (repository.findByUserId(command.userId()).isEmpty()) {
            repository.save(new UserPlan(command.userId()));
        }
    }
}
