package com.claircore.notifications.application.internal.outboundservices.email;

import com.claircore.notifications.domain.model.valueobjects.EmailContent;
import com.claircore.notifications.domain.model.valueobjects.EmailRecipient;
import com.claircore.notifications.domain.model.valueobjects.EmailSubject;

/** Outbound port for email delivery; implemented in {@code infrastructure/communication/smtp}. */
public interface EmailDeliveryService {
    void sendEmail(EmailRecipient recipient, EmailSubject subject, EmailContent content);
}
