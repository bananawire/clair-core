package com.claircore.notifications.application.internal.queryservices;

import com.claircore.notifications.application.queryservices.PushNotificationHistoryQueryService;
import com.claircore.notifications.domain.model.aggregates.PushNotificationLog;
import com.claircore.notifications.domain.model.queries.GetPushNotificationHistoryQuery;
import com.claircore.notifications.domain.repositories.PushNotificationLogRepository;
import com.claircore.shared.domain.model.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PushNotificationHistoryQueryServiceImpl implements PushNotificationHistoryQueryService {

    private final PushNotificationLogRepository pushNotificationLogRepository;

    public PushNotificationHistoryQueryServiceImpl(PushNotificationLogRepository pushNotificationLogRepository) {
        this.pushNotificationLogRepository = pushNotificationLogRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<PushNotificationLog> handle(GetPushNotificationHistoryQuery query) {
        return pushNotificationLogRepository.findByUserId(query.userId(), query.page(), query.size());
    }
}
