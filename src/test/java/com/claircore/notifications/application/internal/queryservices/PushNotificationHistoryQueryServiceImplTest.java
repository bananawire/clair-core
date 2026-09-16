package com.claircore.notifications.application.internal.queryservices;

import com.claircore.notifications.domain.model.aggregates.PushNotificationLog;
import com.claircore.notifications.domain.model.queries.GetPushNotificationHistoryQuery;
import com.claircore.notifications.domain.repositories.PushNotificationLogRepository;
import com.claircore.shared.domain.model.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushNotificationHistoryQueryServiceImplTest {

    @Mock
    private PushNotificationLogRepository pushNotificationLogRepository;

    @InjectMocks
    private PushNotificationHistoryQueryServiceImpl service;

    @Test
    void shouldReturnPushNotificationHistoryForUser() {
        UUID userId = UUID.randomUUID();
        var page = new PageResult<>(List.of(PushNotificationLog.sent(userId, UUID.randomUUID(), "Title", "Message")), 0, 20, 1L);
        when(pushNotificationLogRepository.findByUserId(userId, 0, 20)).thenReturn(page);

        var result = service.handle(new GetPushNotificationHistoryQuery(userId, 0, 20));

        assertEquals(1, result.total());
        assertEquals(1, result.items().size());
        verify(pushNotificationLogRepository).findByUserId(userId, 0, 20);
    }
}
