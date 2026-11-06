package com.claircore.assistant.interfaces.acl;

import java.util.Optional;

/** Published contract for other contexts that need a one-off answer from the assistant's language model. */
public interface AssistantContextFacade {

    /**
     * Sends a system prompt and one user message to the language model, without storing a
     * conversation or consuming any user's quota.
     *
     * @return the model's answer, or empty when the model is not configured or cannot answer
     */
    Optional<String> complete(String systemPrompt, String userMessage);
}
