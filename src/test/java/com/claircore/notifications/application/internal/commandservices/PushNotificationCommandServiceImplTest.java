package com.claircore.notifications.application.internal.commandservices;

import com.claircore.notifications.application.internal.outboundservices.push.PushNotificationDeliveryService;
import com.claircore.notifications.domain.model.aggregates.PushNotificationLog;
import com.claircore.notifications.domain.model.commands.SendPushNotificationCommand;
import com.claircore.notifications.domain.repositories.PushNotificationLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PushNotificationCommandServiceImplTest {

    @Mock
    private PushNotificationDeliveryService pushNotificationDeliveryService;

    @Mock
    private PushNotificationLogRepository pushNotificationLogRepository;

    @InjectMocks
    private PushNotificationCommandServiceImpl service;

    private final UUID userId = UUID.randomUUID();
    private final UUID alertId = UUID.randomUUID();

    @Test
    void shouldDeliverAndLogTheAttemptAsSent() {
        service.handle(new SendPushNotificationCommand(userId, alertId, "Title", "Message"));

        verify(pushNotificationDeliveryService).sendPushNotification(userId, "Title", "Message");
        ArgumentCaptor<PushNotificationLog> captor = ArgumentCaptor.forClass(PushNotificationLog.class);
        verify(pushNotificationLogRepository).save(captor.capture());
        assertTrue(captor.getValue().isSent());
        assertEquals(alertId, captor.getValue().getAlertId());
    }

    @Test
    void shouldLogTheAttemptAsFailedWhenDeliveryThrows() {
        doThrow(new RuntimeException("onesignal down")).when(pushNotificationDeliveryService)
                .sendPushNotification(any(), any(), any());

        service.handle(new SendPushNotificationCommand(userId, alertId, "Title", "Message"));

        ArgumentCaptor<PushNotificationLog> captor = ArgumentCaptor.forClass(PushNotificationLog.class);
        verify(pushNotificationLogRepository).save(captor.capture());
        assertFalse(captor.getValue().isSent());
        assertEquals("onesignal down", captor.getValue().getErrorMessage());
    }
}
