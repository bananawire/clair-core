package com.claircore.assistant.application.internal.outboundservices.llm;

/** One entry of the chat sent to the model, in the OpenAI-compatible role vocabulary. */
public record LanguageModelMessage(String role, String content) {

    public static final String SYSTEM = "system";
    public static final String USER = "user";
    public static final String ASSISTANT = "assistant";

    public LanguageModelMessage {
        if (!SYSTEM.equals(role) && !USER.equals(role) && !ASSISTANT.equals(role)) {
            throw new IllegalArgumentException("Role must be system, user or assistant");
        }
        if (content == null || content.isBlank()) throw new IllegalArgumentException("Content is required");
    }

    public static LanguageModelMessage system(String content) {
        return new LanguageModelMessage(SYSTEM, content);
    }

    public static LanguageModelMessage user(String content) {
        return new LanguageModelMessage(USER, content);
    }

    public static LanguageModelMessage assistant(String content) {
        return new LanguageModelMessage(ASSISTANT, content);
    }
}
