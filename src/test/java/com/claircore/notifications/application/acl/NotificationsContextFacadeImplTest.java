package com.claircore.notifications.application.acl;

import com.claircore.notifications.domain.model.commands.SendVerificationCodeCommand;
import com.claircore.notifications.domain.model.commands.SendWelcomeEmailCommand;
import com.claircore.notifications.application.commandservices.EmailCommandService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationsContextFacadeImplTest {

    @Mock
    private EmailCommandService emailCommandService;

    @InjectMocks
    private NotificationsContextFacadeImpl facade;

    @Test
    void shouldSendWelcomeEmailCommand() {
        facade.sendWelcomeEmail("user@example.com");

        ArgumentCaptor<SendWelcomeEmailCommand> captor = ArgumentCaptor.forClass(SendWelcomeEmailCommand.class);
        verify(emailCommandService).handle(captor.capture());
        assertEquals("user@example.com", captor.getValue().recipient().address());
    }

    @Test
    void shouldSendVerificationCodeCommand() {
        facade.sendVerificationCode("user@example.com", "123456");

        ArgumentCaptor<SendVerificationCodeCommand> captor = ArgumentCaptor.forClass(SendVerificationCodeCommand.class);
        verify(emailCommandService).handle(captor.capture());
        assertEquals("123456", captor.getValue().verificationCode());
    }
}
