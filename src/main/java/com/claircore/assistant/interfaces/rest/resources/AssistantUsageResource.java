package com.claircore.assistant.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Assistant messages sent today against the plan's daily allowance")
public record AssistantUsageResource(
        @Schema(description = "Questions sent today", example = "3") int used,
        @Schema(description = "Daily allowance of the plan", example = "10") int limit,
        @Schema(description = "Questions left today", example = "7") int remaining,
        @Schema(description = "When the count goes back to zero (local midnight)") Instant resetsAt,
        @Schema(description = "Effective plan", example = "FREEMIUM") String plan
) {
}
