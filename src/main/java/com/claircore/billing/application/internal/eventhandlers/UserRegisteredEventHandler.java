package com.claircore.billing.application.internal.eventhandlers;

import com.claircore.billing.application.commandservices.UserPlanCommandService;
import com.claircore.billing.domain.model.commands.InitializeUserPlanCommand;
import com.claircore.billing.domain.model.valueobjects.UserId;
import com.claircore.iam.interfaces.events.UserRegisteredIntegrationEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Service
public class UserRegisteredEventHandler {
    private final UserPlanCommandService commandService;
    public UserRegisteredEventHandler(UserPlanCommandService commandService) { this.commandService = commandService; }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserRegisteredIntegrationEvent event) {
        commandService.handle(new InitializeUserPlanCommand(new UserId(event.userId())));
    }
}
