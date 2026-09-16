package com.claircore.notifications.interfaces.rest;

import com.claircore.iam.application.queryservices.TokenQueryService;
import com.claircore.shared.interfaces.rest.security.CurrentUserIdArgumentResolver;
import com.claircore.notifications.application.queryservices.PushNotificationHistoryQueryService;
import com.claircore.notifications.domain.model.aggregates.PushNotificationLog;
import com.claircore.shared.domain.model.PageResult;
import com.claircore.shared.interfaces.rest.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class NotificationsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PushNotificationHistoryQueryService pushNotificationHistoryQueryService;

    @MockitoBean
    private TokenQueryService tokenQueryService;

    @Test
    void shouldReturnNotificationHistoryForAuthenticatedUser() throws Exception {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655442000");
        var log = PushNotificationLog.reconstitute(
                UUID.randomUUID(), userId, UUID.randomUUID(), "Alert title", "Alert message", true, null,
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
        when(pushNotificationHistoryQueryService.handle(any())).thenReturn(new PageResult<>(List.of(log), 0, 20, 1L));

        mockMvc.perform(get("/api/v1/notifications/push")
                        .requestAttr(CurrentUserIdArgumentResolver.USER_ID_ATTRIBUTE, userId)
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$.content[0].title").value("Alert title"))
                .andExpect(jsonPath("$.content[0].status").value("SENT"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(pushNotificationHistoryQueryService).handle(any());
    }
}
