package com.claircore.nino.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

public record NinoAlertResource(
    @Schema(description = "Alert in plain language with one concrete action")
    String message,

    @Schema(description = "Who wrote the message: the language model or the pre-written fallback",
            allowableValues = {"deepseek", "fallback"})
    String source
) {}
