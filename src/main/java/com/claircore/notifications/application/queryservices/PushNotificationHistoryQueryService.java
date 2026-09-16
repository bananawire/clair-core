package com.claircore.notifications.application.queryservices;

import com.claircore.notifications.domain.model.aggregates.PushNotificationLog;
import com.claircore.notifications.domain.model.queries.GetPushNotificationHistoryQuery;
import com.claircore.shared.domain.model.PageResult;

public interface PushNotificationHistoryQueryService {
    PageResult<PushNotificationLog> handle(GetPushNotificationHistoryQuery query);
}
