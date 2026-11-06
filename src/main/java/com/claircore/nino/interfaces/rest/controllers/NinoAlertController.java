package com.claircore.nino.interfaces.rest.controllers;

import com.claircore.nino.application.internal.commandservices.NinoAlertService;
import com.claircore.nino.domain.model.valueobjects.HomeReadings;
import com.claircore.nino.interfaces.rest.resources.NinoAlertRequest;
import com.claircore.nino.interfaces.rest.resources.NinoAlertResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alerta-nino")
@Tag(name = "El Niño", description = "AI-written early warnings for El Niño health risks at home")
public class NinoAlertController {

    private final NinoAlertService ninoAlertService;

    public NinoAlertController(NinoAlertService ninoAlertService) {
        this.ninoAlertService = ninoAlertService;
    }

    @PostMapping
    @Operation(summary = "Write a plain-language El Niño alert with one concrete action for a home's readings")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alert written by the model, or the pre-written fallback"),
            @ApiResponse(responseCode = "400", description = "A reading is out of range"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<NinoAlertResource> generate(@Valid @RequestBody(required = false) NinoAlertRequest request) {
        var readings = request == null
                ? HomeReadings.empty()
                : new HomeReadings(request.humidity(), request.temperature(), request.riskLevel(), request.riskScore());
        var alert = ninoAlertService.generate(readings);
        return ResponseEntity.ok(new NinoAlertResource(alert.message(), alert.source()));
    }
}
