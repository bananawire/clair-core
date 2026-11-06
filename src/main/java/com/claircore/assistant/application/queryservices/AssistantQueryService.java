package com.claircore.assistant.application.queryservices;

import com.claircore.assistant.domain.model.aggregates.Conversation;
import com.claircore.assistant.domain.model.aggregates.ConversationMessage;
import com.claircore.assistant.domain.model.queries.GetAssistantUsageQuery;
import com.claircore.assistant.domain.model.queries.GetConversationMessagesQuery;
import com.claircore.assistant.domain.model.queries.GetConversationsByUserQuery;
import com.claircore.assistant.domain.model.valueobjects.DailyUsage;
import com.claircore.shared.domain.model.PageResult;

import java.util.List;

public interface AssistantQueryService {

    /** Oldest first. Throws {@code ResourceNotFoundException} for a conversation of someone else. */
    List<ConversationMessage> handle(GetConversationMessagesQuery query);

    PageResult<Conversation> handle(GetConversationsByUserQuery query);

    DailyUsage handle(GetAssistantUsageQuery query);
}
