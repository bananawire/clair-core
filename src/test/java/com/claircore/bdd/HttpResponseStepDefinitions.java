package com.claircore.bdd;

import io.cucumber.java.es.Entonces;

import static org.assertj.core.api.Assertions.assertThat;

/** Steps shared by every feature: the HTTP contract of the last request. */
public class HttpResponseStepDefinitions extends AbstractCucumberSteps {

    @Entonces("la respuesta tiene estado {int}")
    public void laRespuestaTieneEstado(int expectedStatus) {
        assertThat(state.lastResponse).as("no se envió ninguna solicitud").isNotNull();
        assertThat(state.lastResponse.getStatusCode().value())
                .as("cuerpo: %s", state.lastResponse.getBody())
                .isEqualTo(expectedStatus);
    }
}
