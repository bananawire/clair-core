package com.claircore.assistant.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SendAssistantMessageRequest(
    @Schema(description = "Conversation to continue; omit to start a new one", nullable = true)
    UUID conversationId,

    @NotBlank
    @Size(max = 1000)
    @Schema(description = "The user's question", example = "Why is the CO2 high in my office?")
    String content,

    @Pattern(regexp = "es|en", message = "must be es or en")
    @Schema(description = "Answer language", example = "es", allowableValues = {"es", "en"}, nullable = true)
    String locale
) {}
