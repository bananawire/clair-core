package com.claircore.notifications.application.internal.outboundservices.push;

import java.util.UUID;

/** Outbound port for push delivery; implemented in {@code infrastructure/communication/onesignal}. */
public interface PushNotificationDeliveryService {
    void sendPushNotification(UUID userId, String title, String message);
}
