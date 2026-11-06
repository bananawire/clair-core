package com.claircore.nino.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/** Readings of one home; every field is optional and missing ones fall back to the demo scenario. */
public record NinoAlertRequest(
    @DecimalMin("0") @DecimalMax("100")
    @Schema(description = "Relative humidity in percent", example = "85", nullable = true)
    Double humidity,

    @DecimalMin("-20") @DecimalMax("60")
    @Schema(description = "Indoor temperature in °C", example = "30", nullable = true)
    Double temperature,

    @Pattern(regexp = "BAJO|MEDIO|ALTO", message = "must be BAJO, MEDIO or ALTO")
    @Schema(description = "Dengue risk level", example = "ALTO", allowableValues = {"BAJO", "MEDIO", "ALTO"}, nullable = true)
    String riskLevel,

    @Min(0) @Max(100)
    @Schema(description = "Dengue risk score from 0 to 100", example = "82", nullable = true)
    Integer riskScore
) {}
