package com.claircore.notifications.application.internal.commandservices;

import com.claircore.notifications.application.commandservices.PushNotificationCommandService;
import com.claircore.notifications.application.internal.outboundservices.push.PushNotificationDeliveryService;
import com.claircore.notifications.domain.model.aggregates.PushNotificationLog;
import com.claircore.notifications.domain.model.commands.SendPushNotificationCommand;
import com.claircore.notifications.domain.repositories.PushNotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PushNotificationCommandServiceImpl implements PushNotificationCommandService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PushNotificationCommandServiceImpl.class);

    private final PushNotificationDeliveryService pushNotificationDeliveryService;
    private final PushNotificationLogRepository pushNotificationLogRepository;

    public PushNotificationCommandServiceImpl(
            PushNotificationDeliveryService pushNotificationDeliveryService,
            PushNotificationLogRepository pushNotificationLogRepository) {
        this.pushNotificationDeliveryService = pushNotificationDeliveryService;
        this.pushNotificationLogRepository = pushNotificationLogRepository;
    }

    /**
     * A delivery failure is a logged outcome, not an error for the caller: the attempt is recorded
     * as failed and the command completes. This is the behaviour the alert event handler relied on
     * before the split, when it held the same try/catch itself.
     */
    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void handle(SendPushNotificationCommand command) {
        PushNotificationLog attempt;
        try {
            pushNotificationDeliveryService.sendPushNotification(command.userId(), command.title(), command.message());
            attempt = PushNotificationLog.sent(command.userId(), command.alertId(), command.title(), command.message());
        } catch (Exception e) {
            attempt = PushNotificationLog.failed(command.userId(), command.alertId(), command.title(), command.message(), e.getMessage());
            LOGGER.error("Push delivery failed for alert {}", command.alertId(), e);
        }
        // A storage failure must not be mistaken for a delivery failure or saved twice.
        pushNotificationLogRepository.save(attempt);
    }
}
