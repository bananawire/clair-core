package com.claircore.billing.application.commandservices;

import com.claircore.billing.domain.model.commands.InitializeUserPlanCommand;

public interface UserPlanCommandService {
    void handle(InitializeUserPlanCommand command);
}
