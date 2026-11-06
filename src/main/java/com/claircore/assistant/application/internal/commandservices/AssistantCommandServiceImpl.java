package com.claircore.assistant.application.internal.commandservices;

import com.claircore.assistant.application.commandservices.AssistantCommandService;
import com.claircore.assistant.application.commandservices.AssistantReply;
import com.claircore.assistant.application.internal.context.AssistantContextAssembler;
import com.claircore.assistant.application.internal.outboundservices.llm.LanguageModelClient;
import com.claircore.assistant.application.internal.outboundservices.llm.LanguageModelMessage;
import com.claircore.assistant.application.internal.outboundservices.llm.LanguageModelReply;
import com.claircore.assistant.application.internal.usage.AssistantUsageCalculator;
import com.claircore.assistant.domain.exceptions.AssistantQuotaExceededException;
import com.claircore.assistant.domain.model.aggregates.Conversation;
import com.claircore.assistant.domain.model.aggregates.ConversationMessage;
import com.claircore.assistant.domain.model.commands.RateAssistantMessageCommand;
import com.claircore.assistant.domain.model.commands.SendAssistantMessageCommand;
import com.claircore.assistant.domain.model.valueobjects.DailyUsage;
import com.claircore.assistant.domain.repositories.ConversationMessageRepository;
import com.claircore.assistant.domain.repositories.ConversationRepository;
import com.claircore.shared.domain.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class AssistantCommandServiceImpl implements AssistantCommandService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final AssistantUsageCalculator assistantUsageCalculator;
    private final AssistantContextAssembler assistantContextAssembler;
    private final LanguageModelClient languageModelClient;
    private final TransactionTemplate transactionTemplate;
    private final int historyWindow;

    public AssistantCommandServiceImpl(
            ConversationRepository conversationRepository,
            ConversationMessageRepository conversationMessageRepository,
            AssistantUsageCalculator assistantUsageCalculator,
            AssistantContextAssembler assistantContextAssembler,
            LanguageModelClient languageModelClient,
            TransactionTemplate transactionTemplate,
            @Value("${assistant.history-window:10}") int historyWindow) {
        this.conversationRepository = conversationRepository;
        this.conversationMessageRepository = conversationMessageRepository;
        this.assistantUsageCalculator = assistantUsageCalculator;
        this.assistantContextAssembler = assistantContextAssembler;
        this.languageModelClient = languageModelClient;
        this.transactionTemplate = transactionTemplate;
        this.historyWindow = historyWindow;
    }

    /**
     * Not {@code @Transactional} as a whole: the model call can take seconds and must not hold a
     * database connection. Only the final write of the conversation and both turns is one
     * transaction, and it happens after the model answered, so a failed call stores nothing and
     * consumes no quota.
     */
    @Override
    public AssistantReply handle(SendAssistantMessageCommand command) {
        DailyUsage usage = assistantUsageCalculator.currentUsage(command.userId());
        if (usage.isExhausted()) {
            throw new AssistantQuotaExceededException(usage);
        }

        Instant now = assistantUsageCalculator.now();
        Conversation conversation;
        List<ConversationMessage> history;
        if (command.conversationId() != null) {
            conversation = conversationRepository.findByIdAndUserId(command.conversationId(), command.userId())
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
            history = conversationMessageRepository.findRecentByConversationId(conversation.getId(), historyWindow);
        } else {
            conversation = Conversation.start(command.userId(), command.content(), now);
            history = List.of();
        }

        var prompt = new ArrayList<LanguageModelMessage>();
        prompt.add(LanguageModelMessage.system(
                assistantContextAssembler.buildSystemPrompt(command.userId(), command.locale(), now)));
        history.forEach(turn -> prompt.add(turn.isFromAssistant()
                ? LanguageModelMessage.assistant(turn.getContent())
                : LanguageModelMessage.user(turn.getContent())));
        prompt.add(LanguageModelMessage.user(command.content()));

        LanguageModelReply reply = languageModelClient.complete(prompt);

        int questionPosition = conversation.recordExchange(now);
        var question = ConversationMessage.user(
                conversation.getId(), command.userId(), questionPosition, command.content());
        var answer = ConversationMessage.assistant(conversation.getId(), command.userId(), questionPosition + 1,
                reply.content(), reply.model(), reply.promptTokens(), reply.completionTokens(), reply.latencyMs());

        List<ConversationMessage> stored = transactionTemplate.execute(status -> {
            conversationRepository.save(conversation);
            return conversationMessageRepository.saveAll(List.of(question, answer));
        });
        return new AssistantReply(conversation.getId(), stored.get(0), stored.get(1), usage.withOneMoreMessage());
    }

    /** Someone else's message and a user turn both read as not found, so ids cannot be probed. */
    @Override
    @Transactional
    public void handle(RateAssistantMessageCommand command) {
        ConversationMessage message = conversationMessageRepository.findById(command.messageId())
                .filter(found -> found.belongsTo(command.userId()) && found.isFromAssistant())
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        message.rate(command.rating());
        conversationMessageRepository.save(message);
    }
}
