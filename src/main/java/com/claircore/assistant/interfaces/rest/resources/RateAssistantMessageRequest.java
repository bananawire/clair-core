package com.claircore.assistant.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RateAssistantMessageRequest(
    @NotNull
    @Pattern(regexp = "UP|DOWN", message = "must be UP or DOWN")
    @Schema(description = "Verdict on the assistant answer", example = "UP", allowableValues = {"UP", "DOWN"})
    String rating
) {}
