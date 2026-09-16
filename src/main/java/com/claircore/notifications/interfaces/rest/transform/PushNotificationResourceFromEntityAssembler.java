package com.claircore.notifications.interfaces.rest.transform;

import com.claircore.notifications.domain.model.aggregates.PushNotificationLog;
import com.claircore.notifications.interfaces.rest.resources.PushNotificationResource;

public final class PushNotificationResourceFromEntityAssembler {

    private PushNotificationResourceFromEntityAssembler() {
    }

    public static PushNotificationResource toResourceFromEntity(PushNotificationLog notification) {
        return new PushNotificationResource(
                notification.getId(),
                notification.getUserId(),
                notification.getAlertId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isSent() ? "SENT" : "FAILED",
                notification.getErrorMessage(),
                notification.getCreatedAt());
    }
}
