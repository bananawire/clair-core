package com.claircore.assistant.application.commandservices;

import com.claircore.assistant.domain.model.commands.RateAssistantMessageCommand;
import com.claircore.assistant.domain.model.commands.SendAssistantMessageCommand;

public interface AssistantCommandService {

    /**
     * Asks the model and stores the question with its answer. Throws
     * {@code AssistantQuotaExceededException} before calling the model when today's allowance is
     * spent, and {@code AssistantUnavailableException} without storing anything when the model fails.
     */
    AssistantReply handle(SendAssistantMessageCommand command);

    /** Records the user's verdict on one of their assistant answers. */
    void handle(RateAssistantMessageCommand command);
}
