package com.claircore.bdd;

import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;

/** WS-US-03: only a verified user with the right password receives a token. */
public class SignInStepDefinitions extends AbstractCucumberSteps {

    @Dado("que existe un usuario verificado con correo {string} y contraseña {string}")
    public void existeUsuarioVerificado(String email, String password) {
        // The database outlives a single scenario, so the user is created only the first time.
        if (signIn(email, password).getStatusCode().value() != 200) {
            registerAndSignIn(email, password);
        }
        state.accessToken = null;
        state.lastResponse = null;
    }

    @Cuando("inicia sesión con correo {string} y contraseña {string}")
    public void iniciaSesion(String email, String password) {
        signIn(email, password);
    }
}
