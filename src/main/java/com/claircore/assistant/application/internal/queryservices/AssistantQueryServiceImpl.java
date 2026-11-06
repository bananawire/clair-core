package com.claircore.assistant.application.internal.queryservices;

import com.claircore.assistant.application.internal.usage.AssistantUsageCalculator;
import com.claircore.assistant.application.queryservices.AssistantQueryService;
import com.claircore.assistant.domain.model.aggregates.Conversation;
import com.claircore.assistant.domain.model.aggregates.ConversationMessage;
import com.claircore.assistant.domain.model.queries.GetAssistantUsageQuery;
import com.claircore.assistant.domain.model.queries.GetConversationMessagesQuery;
import com.claircore.assistant.domain.model.queries.GetConversationsByUserQuery;
import com.claircore.assistant.domain.model.valueobjects.DailyUsage;
import com.claircore.assistant.domain.repositories.ConversationMessageRepository;
import com.claircore.assistant.domain.repositories.ConversationRepository;
import com.claircore.shared.domain.exceptions.ResourceNotFoundException;
import com.claircore.shared.domain.model.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AssistantQueryServiceImpl implements AssistantQueryService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final AssistantUsageCalculator assistantUsageCalculator;

    public AssistantQueryServiceImpl(
            ConversationRepository conversationRepository,
            ConversationMessageRepository conversationMessageRepository,
            AssistantUsageCalculator assistantUsageCalculator) {
        this.conversationRepository = conversationRepository;
        this.conversationMessageRepository = conversationMessageRepository;
        this.assistantUsageCalculator = assistantUsageCalculator;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationMessage> handle(GetConversationMessagesQuery query) {
        Conversation conversation = conversationRepository.findByIdAndUserId(query.conversationId(), query.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        return conversationMessageRepository.findByConversationId(conversation.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Conversation> handle(GetConversationsByUserQuery query) {
        return conversationRepository.findByUserId(query.userId(), query.page(), query.size());
    }

    @Override
    @Transactional(readOnly = true)
    public DailyUsage handle(GetAssistantUsageQuery query) {
        return assistantUsageCalculator.currentUsage(query.userId());
    }
}
