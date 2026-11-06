package com.claircore.assistant.interfaces.rest.transform;

import com.claircore.assistant.domain.model.commands.SendAssistantMessageCommand;
import com.claircore.assistant.domain.model.valueobjects.AssistantLocale;
import com.claircore.assistant.interfaces.rest.resources.SendAssistantMessageRequest;

import java.util.UUID;

public final class SendAssistantMessageCommandFromResourceAssembler {

    private SendAssistantMessageCommandFromResourceAssembler() {
    }

    public static SendAssistantMessageCommand toCommandFromResource(UUID userId, SendAssistantMessageRequest request) {
        return new SendAssistantMessageCommand(
                userId,
                request.conversationId(),
                request.content(),
                AssistantLocale.fromCode(request.locale()));
    }
}
