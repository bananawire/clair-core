package com.claircore.notifications.domain.repositories;

import com.claircore.notifications.domain.model.aggregates.EmailLog;
import com.claircore.notifications.domain.model.valueobjects.EmailRecipient;

import java.util.List;

/** Port for email log storage. Domain types only. */
public interface EmailLogRepository {

    EmailLog save(EmailLog emailLog);

    List<EmailLog> findByRecipient(EmailRecipient recipient);
}
