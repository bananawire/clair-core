package com.claircore.bdd;

import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** WS-US-24 / WS-US-29 / WS-US-31: organizations group spaces, bounded by the user's plan. */
public class OrganizationSpaceStepDefinitions extends AbstractCucumberSteps {

    private final Map<String, Integer> expectedStatusBySpace = new LinkedHashMap<>();
    private final Map<String, Integer> actualStatusBySpace = new LinkedHashMap<>();

    @Dado("que un administrador de instalaciones con plan {string} ha iniciado sesión")
    public void administradorConPlan(String plan) {
        registerAndSignIn(uniqueEmail("admin"), "Clair@2026");
        assignPlan(state.userId, plan);
    }

    @Cuando("crea la organización {string}")
    public void creaOrganizacion(String name) {
        createOrganization(name);
    }

    @Y("registra los siguientes espacios en la organización:")
    public void registraEspacios(DataTable spaces) {
        for (Map<String, String> row : spaces.asMaps()) {
            String name = row.get("nombre");
            expectedStatusBySpace.put(name, Integer.parseInt(row.get("estado")));
            actualStatusBySpace.put(name, createSpace(state.organizationId, name).getStatusCode().value());
        }
    }

    @Entonces("cada espacio recibe el estado indicado")
    public void cadaEspacioRecibeEstado() {
        assertThat(actualStatusBySpace).containsExactlyEntriesOf(expectedStatusBySpace);
    }

    @Y("la organización {string} aparece en su listado de organizaciones")
    public void organizacionEnListado(String name) {
        JsonNode organizations = json(get("/api/v1/organizations"));
        List<String> names = new ArrayList<>();
        organizations.forEach(org -> names.add(org.get("name").asText()));
        assertThat(names).contains(name);
    }

    @Y("el listado de espacios de la organización contiene:")
    public void listadoDeEspacios(DataTable expected) {
        JsonNode spaces = json(get("/api/v1/spaces?organizationId=" + state.organizationId));
        List<String> names = new ArrayList<>();
        spaces.forEach(space -> names.add(space.get("name").asText()));
        List<String> expectedNames = expected.asMaps().stream().map(row -> row.get("nombre")).toList();
        assertThat(names).containsExactlyInAnyOrderElementsOf(expectedNames);
    }
}
