package com.claircore.assistant.application.internal.commandservices;

import com.claircore.assistant.application.internal.context.AssistantContextAssembler;
import com.claircore.assistant.application.internal.outboundservices.llm.LanguageModelClient;
import com.claircore.assistant.application.internal.outboundservices.llm.LanguageModelMessage;
import com.claircore.assistant.application.internal.outboundservices.llm.LanguageModelReply;
import com.claircore.assistant.application.internal.usage.AssistantUsageCalculator;
import com.claircore.assistant.domain.exceptions.AssistantQuotaExceededException;
import com.claircore.assistant.domain.exceptions.AssistantUnavailableException;
import com.claircore.assistant.domain.model.aggregates.Conversation;
import com.claircore.assistant.domain.model.aggregates.ConversationMessage;
import com.claircore.assistant.domain.model.commands.RateAssistantMessageCommand;
import com.claircore.assistant.domain.model.commands.SendAssistantMessageCommand;
import com.claircore.assistant.domain.model.valueobjects.AssistantLocale;
import com.claircore.assistant.domain.model.valueobjects.DailyUsage;
import com.claircore.assistant.domain.model.valueobjects.FeedbackRating;
import com.claircore.assistant.domain.model.valueobjects.MessageRole;
import com.claircore.assistant.domain.repositories.ConversationMessageRepository;
import com.claircore.assistant.domain.repositories.ConversationRepository;
import com.claircore.shared.domain.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantCommandServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440020");
    private static final Instant NOW = Instant.parse("2026-11-01T20:00:00Z");
    private static final Instant RESETS_AT = Instant.parse("2026-11-02T05:00:00Z");

    @Mock private ConversationRepository conversationRepository;
    @Mock private ConversationMessageRepository messageRepository;
    @Mock private AssistantUsageCalculator usageCalculator;
    @Mock private AssistantContextAssembler contextAssembler;
    @Mock private LanguageModelClient languageModelClient;

    private AssistantCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        var transactionTemplate = new TransactionTemplate(mock(PlatformTransactionManager.class));
        service = new AssistantCommandServiceImpl(conversationRepository, messageRepository, usageCalculator,
                contextAssembler, languageModelClient, transactionTemplate, 10);
    }

    @Test
    void refusesWithoutCallingTheModelWhenTheDailyLimitIsReached() {
        when(usageCalculator.currentUsage(USER_ID)).thenReturn(new DailyUsage("FREEMIUM", 10, 10, RESETS_AT));

        assertThatThrownBy(() -> service.handle(command(null, "Hola")))
                .isInstanceOf(AssistantQuotaExceededException.class)
                .satisfies(ex -> assertThat(((AssistantQuotaExceededException) ex).getUsage().used()).isEqualTo(10));

        verifyNoInteractions(languageModelClient, conversationRepository, messageRepository);
    }

    @Test
    void startsAConversationAndStoresQuestionAndAnswerInOrder() {
        when(usageCalculator.currentUsage(USER_ID)).thenReturn(new DailyUsage("FREEMIUM", 3, 10, RESETS_AT));
        when(usageCalculator.now()).thenReturn(NOW);
        when(contextAssembler.buildSystemPrompt(USER_ID, AssistantLocale.ES, NOW)).thenReturn("RULES AND CONTEXT");
        when(languageModelClient.complete(anyList()))
                .thenReturn(new LanguageModelReply("Ventila la sala.", "deepseek-flash", 300, 20, 900));
        when(messageRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        var reply = service.handle(command(null, "Hay mucho CO2?"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LanguageModelMessage>> prompt = ArgumentCaptor.forClass(List.class);
        verify(languageModelClient).complete(prompt.capture());
        assertThat(prompt.getValue()).extracting(LanguageModelMessage::role).containsExactly("system", "user");
        assertThat(prompt.getValue().getFirst().content()).isEqualTo("RULES AND CONTEXT");

        ArgumentCaptor<Conversation> saved = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(saved.capture());
        assertThat(saved.getValue().getTitle()).isEqualTo("Hay mucho CO2?");
        assertThat(saved.getValue().getMessageCount()).isEqualTo(2);

        assertThat(reply.conversationId()).isEqualTo(saved.getValue().getId());
        assertThat(reply.userMessage().getRole()).isEqualTo(MessageRole.USER);
        assertThat(reply.userMessage().getPosition()).isZero();
        assertThat(reply.assistantMessage().getContent()).isEqualTo("Ventila la sala.");
        assertThat(reply.assistantMessage().getPosition()).isEqualTo(1);
        assertThat(reply.assistantMessage().getModel()).isEqualTo("deepseek-flash");
        assertThat(reply.assistantMessage().getLatencyMs()).isEqualTo(900);
        assertThat(reply.usage().used()).isEqualTo(4);
        assertThat(reply.usage().remaining()).isEqualTo(6);
    }

    @Test
    void continuesAConversationWithItsRecentHistory() {
        UUID conversationId = UUID.randomUUID();
        var existing = Conversation.reconstitute(conversationId, USER_ID, "Earlier", NOW.minusSeconds(600), 2,
                NOW.minusSeconds(600), NOW.minusSeconds(600));
        when(usageCalculator.currentUsage(USER_ID)).thenReturn(new DailyUsage("PREMIUM", 1, 100, RESETS_AT));
        when(usageCalculator.now()).thenReturn(NOW);
        when(conversationRepository.findByIdAndUserId(conversationId, USER_ID)).thenReturn(Optional.of(existing));
        when(messageRepository.findRecentByConversationId(conversationId, 10)).thenReturn(List.of(
                ConversationMessage.user(conversationId, USER_ID, 0, "Earlier question"),
                ConversationMessage.assistant(conversationId, USER_ID, 1, "Earlier answer", "m", 1, 1, 1)));
        when(contextAssembler.buildSystemPrompt(any(), any(), any())).thenReturn("CONTEXT");
        when(languageModelClient.complete(anyList())).thenReturn(new LanguageModelReply("Sure.", "m", 1, 1, 1));
        when(messageRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        var reply = service.handle(command(conversationId, "And now?"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LanguageModelMessage>> prompt = ArgumentCaptor.forClass(List.class);
        verify(languageModelClient).complete(prompt.capture());
        assertThat(prompt.getValue()).extracting(LanguageModelMessage::role)
                .containsExactly("system", "user", "assistant", "user");
        assertThat(prompt.getValue().getLast().content()).isEqualTo("And now?");
        assertThat(reply.conversationId()).isEqualTo(conversationId);
        assertThat(reply.userMessage().getPosition()).isEqualTo(2);
        assertThat(reply.assistantMessage().getPosition()).isEqualTo(3);
        assertThat(existing.getLastMessageAt()).isEqualTo(NOW);
    }

    @Test
    void treatsSomeoneElsesConversationAsNotFoundWithoutCallingTheModel() {
        UUID conversationId = UUID.randomUUID();
        when(usageCalculator.currentUsage(USER_ID)).thenReturn(new DailyUsage("FREEMIUM", 0, 10, RESETS_AT));
        when(usageCalculator.now()).thenReturn(NOW);
        when(conversationRepository.findByIdAndUserId(conversationId, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(command(conversationId, "Hola")))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(languageModelClient);
        verify(messageRepository, never()).saveAll(anyList());
    }

    @Test
    void storesNothingWhenTheModelFails() {
        when(usageCalculator.currentUsage(USER_ID)).thenReturn(new DailyUsage("FREEMIUM", 0, 10, RESETS_AT));
        when(usageCalculator.now()).thenReturn(NOW);
        when(contextAssembler.buildSystemPrompt(any(), any(), any())).thenReturn("CONTEXT");
        when(languageModelClient.complete(anyList())).thenThrow(new AssistantUnavailableException("down"));

        assertThatThrownBy(() -> service.handle(command(null, "Hola")))
                .isInstanceOf(AssistantUnavailableException.class);

        verify(conversationRepository, never()).save(any());
        verify(messageRepository, never()).saveAll(anyList());
    }

    @Test
    void ratesTheUsersOwnAssistantAnswer() {
        var answer = ConversationMessage.assistant(UUID.randomUUID(), USER_ID, 1, "Answer", "m", 1, 1, 1);
        when(messageRepository.findById(answer.getId())).thenReturn(Optional.of(answer));

        service.handle(new RateAssistantMessageCommand(USER_ID, answer.getId(), FeedbackRating.UP));

        verify(messageRepository).save(answer);
        assertThat(answer.getFeedback()).isEqualTo(FeedbackRating.UP);
    }

    @Test
    void ratingAQuestionOrSomeoneElsesAnswerReadsAsNotFound() {
        var question = ConversationMessage.user(UUID.randomUUID(), USER_ID, 0, "Question");
        var foreignAnswer = ConversationMessage.assistant(UUID.randomUUID(), UUID.randomUUID(), 1, "Answer", "m", 1, 1, 1);
        when(messageRepository.findById(question.getId())).thenReturn(Optional.of(question));
        when(messageRepository.findById(foreignAnswer.getId())).thenReturn(Optional.of(foreignAnswer));

        assertThatThrownBy(() -> service.handle(new RateAssistantMessageCommand(USER_ID, question.getId(), FeedbackRating.UP)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.handle(new RateAssistantMessageCommand(USER_ID, foreignAnswer.getId(), FeedbackRating.DOWN)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(messageRepository, never()).save(any());
    }

    private static SendAssistantMessageCommand command(UUID conversationId, String content) {
        return new SendAssistantMessageCommand(USER_ID, conversationId, content, AssistantLocale.ES);
    }
}
