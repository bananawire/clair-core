package com.claircore.assistant.application.internal.outboundservices.llm;

/**
 * The model's answer plus what it cost: token counts come from the provider, latency is measured
 * around the call.
 */
public record LanguageModelReply(String content, String model, Integer promptTokens, Integer completionTokens,
                                 int latencyMs) {
    public LanguageModelReply {
        if (content == null || content.isBlank()) throw new IllegalArgumentException("Content is required");
        if (latencyMs < 0) throw new IllegalArgumentException("Latency must not be negative");
    }
}
