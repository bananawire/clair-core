package com.claircore.nino.application.internal.commandservices;

import com.claircore.nino.application.internal.outboundservices.acl.ExternalAssistantService;
import com.claircore.nino.domain.model.valueobjects.HomeReadings;
import com.claircore.nino.domain.model.valueobjects.NinoAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Turns a home's readings into a short El Niño alert written by the language model.
 *
 * <p>Reaches the assistant's language model through its published ACL, so there is a single
 * DeepSeek integration. The model gets {@value #TIMEOUT_SECONDS} seconds; on any failure, empty
 * answer or timeout the caller still gets a usable, pre-written alert marked {@code fallback}, so
 * the demo never shows an error.
 */
@Service
public class NinoAlertService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NinoAlertService.class);
    private static final long TIMEOUT_SECONDS = 8;

    // Demo scenario agreed by the team: a home in Piura during a coastal El Niño
    private static final double DEFAULT_HUMIDITY = 85;
    private static final double DEFAULT_TEMPERATURE = 30;
    private static final String DEFAULT_RISK_LEVEL = "ALTO";
    private static final int DEFAULT_RISK_SCORE = 82;

    private static final String OFFICIAL_SOURCES_NOTICE = "Siga las alertas oficiales de MINSA y SENAMHI.";

    static final String FALLBACK_MESSAGE =
            "Alerta preventiva: el calor y la humedad de tu casa favorecen al zancudo del dengue. "
                    + "Hoy mismo vacía o tapa baldes, botellas y macetas con agua, y toma agua seguido si hay "
                    + "niños o adultos mayores. " + OFFICIAL_SOURCES_NOTICE;

    private static final String SYSTEM_PROMPT = """
            Eres un sistema de alerta temprana del Fenómeno del Niño en Perú.
            Con los datos de una vivienda, escribe una alerta de máximo 2 oraciones, en español sencillo,
            para una familia sin conocimientos técnicos. Incluye una sola acción concreta para prevenir
            el dengue o el golpe de calor. Si hay datos de lluvia o nivel de agua y son altos, prioriza
            la acción ante inundación o huaico. No inventes cifras ni cites fuentes que no se te dieron.
            Termina exactamente con: "%s"
            """.formatted(OFFICIAL_SOURCES_NOTICE);

    private final ExternalAssistantService externalAssistantService;

    public NinoAlertService(ExternalAssistantService externalAssistantService) {
        this.externalAssistantService = externalAssistantService;
    }

    public NinoAlert generate(HomeReadings readings) {
        String userMessage = describe(readings == null ? HomeReadings.empty() : readings);
        try {
            var answer = CompletableFuture
                    .supplyAsync(() -> externalAssistantService.complete(SYSTEM_PROMPT, userMessage))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (answer.isEmpty() || answer.get().isBlank()) {
                return fallback(new IllegalStateException("Language model unavailable"));
            }
            return new NinoAlert(answer.get(), NinoAlert.SOURCE_MODEL);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fallback(e);
        } catch (Exception e) {
            return fallback(e);
        }
    }

    private static NinoAlert fallback(Exception cause) {
        LOGGER.warn("El Niño alert fell back to the pre-written message: {}", cause.getClass().getSimpleName());
        return new NinoAlert(FALLBACK_MESSAGE, NinoAlert.SOURCE_FALLBACK);
    }

    static String describe(HomeReadings r) {
        double humidity = r.humidity() != null ? r.humidity() : DEFAULT_HUMIDITY;
        double temperature = r.temperature() != null ? r.temperature() : DEFAULT_TEMPERATURE;
        String riskLevel = r.riskLevel() != null ? r.riskLevel() : DEFAULT_RISK_LEVEL;
        int riskScore = r.riskScore() != null ? r.riskScore() : DEFAULT_RISK_SCORE;
        String readings = String.format(Locale.ROOT,
                "Vivienda en la costa norte del Perú durante El Niño costero. Humedad: %.0f %%. "
                        + "Temperatura interior: %.1f °C. Riesgo de dengue: %s (%d/100).",
                humidity, temperature, riskLevel, riskScore);
        // Hydrology only exists on outdoor stations, so it is mentioned only when sent
        if (r.rainfallMm() != null) {
            readings += String.format(Locale.ROOT, " Lluvia en el último intervalo: %.1f mm.", r.rainfallMm());
        }
        if (r.waterLevelCm() != null) {
            readings += String.format(Locale.ROOT, " Nivel de agua cerca de la vivienda: %.0f cm.", r.waterLevelCm());
        }
        return readings;
    }
}
