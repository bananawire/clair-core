package com.claircore.notifications.application.internal.commandservices;

import com.claircore.notifications.domain.model.commands.SendVerificationCodeCommand;
import com.claircore.notifications.domain.model.commands.SendWelcomeEmailCommand;
import com.claircore.notifications.domain.model.aggregates.EmailLog;
import com.claircore.notifications.domain.model.valueobjects.EmailContent;
import com.claircore.notifications.domain.model.valueobjects.EmailRecipient;
import com.claircore.notifications.domain.model.valueobjects.EmailSubject;
import com.claircore.notifications.domain.repositories.EmailLogRepository;
import com.claircore.notifications.application.internal.outboundservices.email.EmailDeliveryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailCommandServiceImplTest {

    @Mock
    private EmailDeliveryService emailDeliveryService;

    @Mock
    private EmailLogRepository emailLogRepository;

    @InjectMocks
    private EmailCommandServiceImpl service;

    @Test
    void shouldSendAndLogWelcomeEmail() {
        service.handle(new SendWelcomeEmailCommand(new EmailRecipient("user@example.com")));

        ArgumentCaptor<EmailLog> captor = ArgumentCaptor.forClass(EmailLog.class);
        verify(emailDeliveryService).sendEmail(any(EmailRecipient.class), any(EmailSubject.class), any(EmailContent.class));
        verify(emailLogRepository).save(captor.capture());
        assertTrue(captor.getValue().isSent());
    }

    @Test
    void shouldLogFailedEmailWhenDeliveryThrows() {
        doThrow(new RuntimeException("smtp down")).when(emailDeliveryService).sendEmail(any(), any(), any());

        service.handle(new SendVerificationCodeCommand(new EmailRecipient("user@example.com"), "123456"));

        ArgumentCaptor<EmailLog> captor = ArgumentCaptor.forClass(EmailLog.class);
        verify(emailLogRepository).save(captor.capture());
        assertFalse(captor.getValue().isSent());
    }
}
