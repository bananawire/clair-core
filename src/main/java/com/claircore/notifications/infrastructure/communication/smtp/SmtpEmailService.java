package com.claircore.notifications.infrastructure.communication.smtp;

import com.claircore.notifications.domain.model.valueobjects.EmailContent;
import com.claircore.notifications.domain.model.valueobjects.EmailRecipient;
import com.claircore.notifications.domain.model.valueobjects.EmailSubject;
import com.claircore.notifications.application.internal.outboundservices.email.EmailDeliveryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class SmtpEmailService implements EmailDeliveryService {

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public SmtpEmailService(JavaMailSender mailSender,
                            @Value("${SMTP_FROM}") String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    @Override
    public void sendEmail(EmailRecipient recipient, EmailSubject subject, EmailContent content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(recipient.address());
            helper.setSubject(subject.value());
            helper.setText(content.html(), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email via SMTP", e);
        }
    }
}
