package com.claircore.notifications.application.commandservices;

import com.claircore.notifications.domain.model.commands.SendVerificationCodeCommand;
import com.claircore.notifications.domain.model.commands.SendWelcomeEmailCommand;

public interface EmailCommandService {
    void handle(SendWelcomeEmailCommand command);
    void handle(SendVerificationCodeCommand command);
}
