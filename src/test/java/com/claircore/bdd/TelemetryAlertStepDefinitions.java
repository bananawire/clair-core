package com.claircore.bdd;

import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** WS-US-49 / WS-US-34: recorded telemetry is evaluated against thresholds and opens alerts. */
public class TelemetryAlertStepDefinitions extends AbstractCucumberSteps {

    @Dado("que el usuario tiene un dispositivo Clair reclamado con umbral de {string} en {string}")
    public void dispositivoConUmbral(String metric, String value) {
        userWithClaimedDevice();
        var created = post("/api/v1/devices/" + state.deviceId + "/thresholds",
                Map.of("metric", metric, "value", new BigDecimal(value), "enabled", true));
        assertThat(created.getStatusCode().value()).isEqualTo(201);
    }

    @Cuando("el dispositivo envía las siguientes lecturas:")
    public void enviaLecturas(DataTable readings) {
        // Readings must be measured after the claim to be visible to the new owner.
        Instant measuredAt = Instant.now().truncatedTo(ChronoUnit.SECONDS).plusSeconds(1);
        int uptime = 20;
        for (Map<String, String> row : readings.asMaps()) {
            var response = post("/api/v1/evaluations/telemetry", reading(row, measuredAt, uptime));
            assertThat(response.getStatusCode().value()).as("cuerpo: %s", response.getBody()).isEqualTo(201);
            measuredAt = measuredAt.plusSeconds(15);
            uptime += 15;
        }
    }

    @Entonces("se registran {int} lecturas de telemetría para el dispositivo")
    public void seRegistranLecturas(int expected) {
        JsonNode page = json(get("/api/v1/evaluations/devices/" + state.deviceId));
        assertThat(page.get("totalElements").asInt()).isEqualTo(expected);
    }

    @Y("el usuario tiene {int} alerta(s) activa(s) de {string} con severidad {string}")
    public void tieneAlertasActivas(int expected, String metric, String severity) {
        JsonNode alerts = json(get("/api/v1/alerts?status=ACTIVE")).get("content");
        long matching = 0;
        for (JsonNode alert : alerts) {
            if (state.deviceId.toString().equals(alert.get("deviceId").asText())
                    && metric.equals(alert.get("metric").asText())
                    && severity.equals(alert.get("severity").asText())) {
                matching++;
            }
        }
        assertThat(matching).isEqualTo(expected);
    }

    @Y("el usuario no tiene alertas registradas")
    public void sinAlertas() {
        JsonNode page = json(get("/api/v1/alerts"));
        assertThat(page.get("totalElements").asInt()).isZero();
    }

    private Map<String, Object> reading(Map<String, String> row, Instant measuredAt, int uptime) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("deviceId", state.deviceId.toString());
        body.put("readingId", UUID.randomUUID().toString());
        body.put("uptime", Integer.toString(uptime));
        body.put("airQuality", Map.of(
                "co2", Double.parseDouble(row.get("co2")),
                "temperature", Double.parseDouble(row.get("temperatura")),
                "humidity", Double.parseDouble(row.get("humedad"))));
        body.put("particulateMatter", Map.of(
                "pm1_0", 5.0,
                "pm2_5", Double.parseDouble(row.get("pm25")),
                "pm10", 25.0));
        body.put("connectivity", Map.of("status", "connected", "network", "Clair-Lab", "signalStrength", -60));
        body.put("location", Map.of("country", "PERU"));
        body.put("healthStatus", 100);
        body.put("status", "Optimal");
        body.put("measuredAt", measuredAt.toString());
        return body;
    }
}
