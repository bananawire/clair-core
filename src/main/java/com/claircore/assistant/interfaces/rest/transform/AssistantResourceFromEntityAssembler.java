package com.claircore.assistant.interfaces.rest.transform;

import com.claircore.assistant.application.commandservices.AssistantReply;
import com.claircore.assistant.domain.model.aggregates.Conversation;
import com.claircore.assistant.domain.model.aggregates.ConversationMessage;
import com.claircore.assistant.domain.model.valueobjects.DailyUsage;
import com.claircore.assistant.interfaces.rest.resources.AssistantReplyResource;
import com.claircore.assistant.interfaces.rest.resources.AssistantUsageResource;
import com.claircore.assistant.interfaces.rest.resources.ConversationMessageResource;
import com.claircore.assistant.interfaces.rest.resources.ConversationResource;

public final class AssistantResourceFromEntityAssembler {

    private AssistantResourceFromEntityAssembler() {
    }

    public static ConversationMessageResource toResourceFromEntity(ConversationMessage message) {
        return new ConversationMessageResource(
                message.getId(),
                message.getConversationId(),
                message.getRole().name(),
                message.getContent(),
                message.getFeedback() == null ? null : message.getFeedback().name(),
                message.getCreatedAt());
    }

    public static ConversationResource toResourceFromEntity(Conversation conversation) {
        return new ConversationResource(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getLastMessageAt(),
                conversation.getCreatedAt());
    }

    public static AssistantUsageResource toResourceFromEntity(DailyUsage usage) {
        return new AssistantUsageResource(
                usage.used(),
                usage.limit(),
                usage.remaining(),
                usage.resetsAt(),
                usage.plan());
    }

    public static AssistantReplyResource toResourceFromEntity(AssistantReply reply) {
        return new AssistantReplyResource(
                reply.conversationId(),
                toResourceFromEntity(reply.userMessage()),
                toResourceFromEntity(reply.assistantMessage()),
                toResourceFromEntity(reply.usage()));
    }
}
