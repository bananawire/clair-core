package com.claircore.notifications.domain.repositories;

import com.claircore.notifications.domain.model.aggregates.PushNotificationLog;
import com.claircore.shared.domain.model.PageResult;

import java.util.UUID;

/** Port for push notification log storage. Domain types only. */
public interface PushNotificationLogRepository {

    PushNotificationLog save(PushNotificationLog pushNotificationLog);

    /** Most recent first; ordering is the adapter's responsibility. */
    PageResult<PushNotificationLog> findByUserId(UUID userId, int page, int size);
}
