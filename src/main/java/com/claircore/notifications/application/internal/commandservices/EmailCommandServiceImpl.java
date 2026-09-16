package com.claircore.notifications.application.internal.commandservices;

import com.claircore.notifications.application.commandservices.EmailCommandService;
import com.claircore.notifications.application.internal.outboundservices.email.EmailDeliveryService;
import com.claircore.notifications.domain.model.aggregates.EmailLog;
import com.claircore.notifications.domain.model.commands.SendVerificationCodeCommand;
import com.claircore.notifications.domain.model.commands.SendWelcomeEmailCommand;
import com.claircore.notifications.domain.model.valueobjects.EmailContent;
import com.claircore.notifications.domain.model.valueobjects.EmailRecipient;
import com.claircore.notifications.domain.model.valueobjects.EmailSubject;
import com.claircore.notifications.domain.repositories.EmailLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailCommandServiceImpl implements EmailCommandService {

    private static final Logger logger = LoggerFactory.getLogger(EmailCommandServiceImpl.class);

    private final EmailDeliveryService emailDeliveryService;
    private final EmailLogRepository emailLogRepository;

    public EmailCommandServiceImpl(EmailDeliveryService emailDeliveryService, EmailLogRepository emailLogRepository) {
        this.emailDeliveryService = emailDeliveryService;
        this.emailLogRepository = emailLogRepository;
    }

    @Override
    @Transactional
    public void handle(SendWelcomeEmailCommand command) {
        EmailSubject subject = new EmailSubject("Welcome to Clair IOT Platform!");
        EmailContent content = new EmailContent("<h1>Welcome!</h1><p>We are glad to have you on board.</p>");
        sendAndLog(command.recipient(), subject, content);
    }

    @Override
    @Transactional
    public void handle(SendVerificationCodeCommand command) {
        EmailSubject subject = new EmailSubject("Your Clair IOT Verification Code");
        EmailContent content = new EmailContent(String.format(
                "<h1>Verification Code</h1><p>Your verification code is: <strong>%s</strong></p><p>This code will expire in 30 minutes.</p>",
                command.verificationCode()
        ));
        sendAndLog(command.recipient(), subject, content);
    }

    private void sendAndLog(EmailRecipient recipient, EmailSubject subject, EmailContent content) {
        try {
            emailDeliveryService.sendEmail(recipient, subject, content);
            emailLogRepository.save(EmailLog.sent(recipient, subject, content));
            logger.info("Email sent successfully to {}", recipient.address());
        } catch (Exception e) {
            emailLogRepository.save(EmailLog.failed(recipient, subject, content, e.getMessage()));
            logger.error("Failed to send email to {}: {}", recipient.address(), e.getMessage());
        }
    }
}
