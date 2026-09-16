package com.claircore.notifications.application.commandservices;

import com.claircore.notifications.domain.model.commands.SendPushNotificationCommand;

public interface PushNotificationCommandService {

    /** Delivers the notification and records the attempt, whether it succeeded or not. */
    void handle(SendPushNotificationCommand command);
}
