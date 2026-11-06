package com.claircore.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Registration Initiated Resource")
public record RegistrationInitiatedResource(
    @Schema(description = "Registration session ID")
    String sessionId,
    @Schema(description = "Status message")
    String message,
    @Schema(description = "Whether the user must confirm an email verification code")
    boolean verificationRequired
) {}
