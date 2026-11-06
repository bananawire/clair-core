package com.claircore.bdd;

import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Y;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** WS-US-18 / WS-US-19: one configurable threshold per metric on an owned device. */
public class DeviceThresholdStepDefinitions extends AbstractCucumberSteps {

    @Dado("que el usuario tiene un dispositivo Clair reclamado en su espacio")
    public void dispositivoReclamado() {
        userWithClaimedDevice();
    }

    @Cuando("crea un umbral para la métrica {string} con valor {string}")
    public void creaUmbral(String metric, String value) {
        post(thresholdsPath(), thresholdBody(metric, value));
    }

    @Cuando("actualiza el umbral de la métrica {string} al valor {string}")
    public void actualizaUmbral(String metric, String value) {
        put(thresholdsPath(), thresholdBody(metric, value));
    }

    @Y("el dispositivo tiene un umbral activo de {string} con valor {string}")
    public void umbralActivo(String metric, String value) {
        JsonNode match = null;
        for (JsonNode threshold : json(get(thresholdsPath()))) {
            if (metric.equals(threshold.get("metric").asText())) {
                match = threshold;
            }
        }
        assertThat(match).as("umbral de %s", metric).isNotNull();
        assertThat(match.get("enabled").asBoolean()).isTrue();
        assertThat(new BigDecimal(match.get("value").asText())).isEqualByComparingTo(new BigDecimal(value));
    }

    private String thresholdsPath() {
        return "/api/v1/devices/" + state.deviceId + "/thresholds";
    }

    private Map<String, Object> thresholdBody(String metric, String value) {
        return Map.of("metric", metric, "value", new BigDecimal(value), "enabled", true);
    }
}
