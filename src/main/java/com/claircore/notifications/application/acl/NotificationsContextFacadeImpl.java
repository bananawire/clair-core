package com.claircore.notifications.application.acl;

import com.claircore.notifications.application.commandservices.EmailCommandService;
import com.claircore.notifications.domain.model.commands.SendVerificationCodeCommand;
import com.claircore.notifications.domain.model.commands.SendWelcomeEmailCommand;
import com.claircore.notifications.domain.model.valueobjects.EmailRecipient;
import com.claircore.notifications.interfaces.acl.NotificationsContextFacade;
import org.springframework.stereotype.Service;

@Service
public class NotificationsContextFacadeImpl implements NotificationsContextFacade {

    private final EmailCommandService emailCommandService;

    public NotificationsContextFacadeImpl(EmailCommandService emailCommandService) {
        this.emailCommandService = emailCommandService;
    }

    @Override
    public void sendWelcomeEmail(String emailAddress) {
        emailCommandService.handle(new SendWelcomeEmailCommand(new EmailRecipient(emailAddress)));
    }

    @Override
    public void sendVerificationCode(String emailAddress, String code) {
        emailCommandService.handle(new SendVerificationCodeCommand(new EmailRecipient(emailAddress), code));
    }
}
