package com.claircore.assistant.application.internal.outboundservices.llm;

import java.util.List;

/**
 * Outbound port for the chat model; implemented in {@code infrastructure/llm/deepseek}.
 *
 * <p>Throws {@code AssistantUnavailableException} when no usable answer can be produced, so callers
 * never persist a half-finished exchange.
 */
public interface LanguageModelClient {
    LanguageModelReply complete(List<LanguageModelMessage> messages);
}
