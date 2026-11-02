package com.claircore.assistant.interfaces.rest.controllers;

import com.claircore.assistant.application.commandservices.AssistantCommandService;
import com.claircore.assistant.application.commandservices.AssistantReply;
import com.claircore.assistant.application.queryservices.AssistantQueryService;
import com.claircore.assistant.domain.exceptions.AssistantQuotaExceededException;
import com.claircore.assistant.domain.exceptions.AssistantUnavailableException;
import com.claircore.assistant.domain.model.aggregates.Conversation;
import com.claircore.assistant.domain.model.aggregates.ConversationMessage;
import com.claircore.assistant.domain.model.commands.RateAssistantMessageCommand;
import com.claircore.assistant.domain.model.commands.SendAssistantMessageCommand;
import com.claircore.assistant.domain.model.queries.GetAssistantUsageQuery;
import com.claircore.assistant.domain.model.queries.GetConversationsByUserQuery;
import com.claircore.assistant.domain.model.valueobjects.AssistantLocale;
import com.claircore.assistant.domain.model.valueobjects.DailyUsage;
import com.claircore.assistant.domain.model.valueobjects.FeedbackRating;
import com.claircore.assistant.interfaces.rest.AssistantExceptionHandler;
import com.claircore.shared.domain.exceptions.ResourceNotFoundException;
import com.claircore.shared.domain.model.PageResult;
import com.claircore.shared.interfaces.rest.security.CurrentUserIdArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AssistantController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class}
)
@Import(AssistantExceptionHandler.class)
class AssistantControllerTest {

    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440050");
    private static final Instant RESETS_AT = Instant.now().plusSeconds(3_600);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssistantCommandService assistantCommandService;

    @MockitoBean
    private AssistantQueryService assistantQueryService;

    @MockitoBean
    private com.claircore.iam.application.queryservices.TokenQueryService tokenQueryService;

    @MockitoBean
    private com.claircore.iam.infrastructure.tokens.jwt.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private com.claircore.iam.infrastructure.config.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @BeforeEach
    void setUpSecurity() throws Exception {
        org.mockito.Mockito.doAnswer(invocation -> {
            jakarta.servlet.ServletRequest request = invocation.getArgument(0);
            jakarta.servlet.ServletResponse response = invocation.getArgument(1);
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    void sendMessageReturnsBothTurnsAndTheAllowanceLeft() throws Exception {
        UUID conversationId = UUID.randomUUID();
        var question = ConversationMessage.user(conversationId, USER_ID, 0, "Is my air OK?");
        var answer = ConversationMessage.assistant(conversationId, USER_ID, 1, "Yes, ventilation is fine.", "deepseek-flash", 200, 12, 800);
        when(assistantCommandService.handle(any(SendAssistantMessageCommand.class))).thenReturn(
                new AssistantReply(conversationId, question, answer, new DailyUsage("FREEMIUM", 4, 10, RESETS_AT)));

        mockMvc.perform(post("/api/v1/assistant/messages")
                        .requestAttr(CurrentUserIdArgumentResolver.USER_ID_ATTRIBUTE, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Is my air OK?\",\"locale\":\"en\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(conversationId.toString()))
                .andExpect(jsonPath("$.userMessage.role").value("USER"))
                .andExpect(jsonPath("$.assistantMessage.content").value("Yes, ventilation is fine."))
                .andExpect(jsonPath("$.assistantMessage.feedback").doesNotExist())
                .andExpect(jsonPath("$.usage.used").value(4))
                .andExpect(jsonPath("$.usage.remaining").value(6))
                .andExpect(jsonPath("$.usage.plan").value("FREEMIUM"));

        ArgumentCaptor<SendAssistantMessageCommand> command = ArgumentCaptor.forClass(SendAssistantMessageCommand.class);
        verify(assistantCommandService).handle(command.capture());
        assertThat(command.getValue().userId()).isEqualTo(USER_ID);
        assertThat(command.getValue().conversationId()).isNull();
        assertThat(command.getValue().locale()).isEqualTo(AssistantLocale.EN);
    }

    @Test
    void sendMessageRejectsBlankOrTooLongContentAndUnknownLocales() throws Exception {
        String tooLong = "x".repeat(1001);
        for (String body : List.of(
                "{\"content\":\"   \"}",
                "{\"content\":\"" + tooLong + "\"}",
                "{\"content\":\"Hola\",\"locale\":\"fr\"}",
                "{not json")) {
            mockMvc.perform(post("/api/v1/assistant/messages")
                            .requestAttr(CurrentUserIdArgumentResolver.USER_ID_ATTRIBUTE, USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
        verify(assistantCommandService, never()).handle(any(SendAssistantMessageCommand.class));
    }

    @Test
    void sendMessageAnswersTooManyRequestsWithRetryAfterWhenTheQuotaIsSpent() throws Exception {
        when(assistantCommandService.handle(any(SendAssistantMessageCommand.class)))
                .thenThrow(new AssistantQuotaExceededException(new DailyUsage("FREEMIUM", 10, 10, RESETS_AT)));

        mockMvc.perform(post("/api/v1/assistant/messages")
                        .requestAttr(CurrentUserIdArgumentResolver.USER_ID_ATTRIBUTE, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hola\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value("Daily assistant message limit reached"));
    }

    @Test
    void sendMessageAnswersServiceUnavailableWhenTheModelFails() throws Exception {
        when(assistantCommandService.handle(any(SendAssistantMessageCommand.class)))
                .thenThrow(new AssistantUnavailableException("Assistant is temporarily unavailable"));

        mockMvc.perform(post("/api/v1/assistant/messages")
                        .requestAttr(CurrentUserIdArgumentResolver.USER_ID_ATTRIBUTE, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hola\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    void sendMessageAnswersNotFoundForSomeoneElsesConversation() throws Exception {
        when(assistantCommandService.handle(any(SendAssistantMessageCommand.class)))
                .thenThrow(new ResourceNotFoundException("Conversation not found"));

        mockMvc.perform(post("/api/v1/assistant/messages")
                        .requestAttr(CurrentUserIdArgumentResolver.USER_ID_ATTRIBUTE, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conversationId\":\"" + UUID.randomUUID() + "\",\"content\":\"Hola\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void usageReportsTodaysAllowance() throws Exception {
        when(assistantQueryService.handle(new GetAssistantUsageQuery(USER_ID)))
                .thenReturn(new DailyUsage("PREMIUM", 30, 100, RESETS_AT));

        mockMvc.perform(get("/api/v1/assistant/usage")
                        .requestAttr(CurrentUserIdArgumentResolver.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.used").value(30))
                .andExpect(jsonPath("$.limit").value(100))
                .andExpect(jsonPath("$.remaining").value(70))
                .andExpect(jsonPath("$.plan").value("PREMIUM"));
    }

    @Test
    void conversationsArePagedAndThePageSizeIsCapped() throws Exception {
        var conversation = Conversation.reconstitute(UUID.randomUUID(), USER_ID, "Why is CO2 high?",
                Instant.parse("2026-11-01T20:00:00Z"), 2, Instant.parse("2026-11-01T20:00:00Z"), Instant.parse("2026-11-01T20:00:00Z"));
        when(assistantQueryService.handle(new GetConversationsByUserQuery(USER_ID, 0, 50)))
                .thenReturn(new PageResult<>(List.of(conversation), 0, 50, 1));

        mockMvc.perform(get("/api/v1/assistant/conversations?size=500")
                        .requestAttr(CurrentUserIdArgumentResolver.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Why is CO2 high?"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void rateMessageAcceptsUpOrDown() throws Exception {
        UUID messageId = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/assistant/messages/" + messageId + "/feedback")
                        .requestAttr(CurrentUserIdArgumentResolver.USER_ID_ATTRIBUTE, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":\"DOWN\"}"))
                .andExpect(status().isNoContent());

        verify(assistantCommandService).handle(new RateAssistantMessageCommand(USER_ID, messageId, FeedbackRating.DOWN));
    }

    @Test
    void rateMessageRejectsUnknownRatingsAndHidesForeignMessages() throws Exception {
        UUID messageId = UUID.randomUUID();
        mockMvc.perform(put("/api/v1/assistant/messages/" + messageId + "/feedback")
                        .requestAttr(CurrentUserIdArgumentResolver.USER_ID_ATTRIBUTE, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":\"MEH\"}"))
                .andExpect(status().isBadRequest());

        doThrow(new ResourceNotFoundException("Message not found"))
                .when(assistantCommandService).handle(any(RateAssistantMessageCommand.class));
        mockMvc.perform(put("/api/v1/assistant/messages/" + messageId + "/feedback")
                        .requestAttr(CurrentUserIdArgumentResolver.USER_ID_ATTRIBUTE, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":\"UP\"}"))
                .andExpect(status().isNotFound());
    }
}
