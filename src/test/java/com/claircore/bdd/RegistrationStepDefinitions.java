package com.claircore.bdd;

import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Y;

import static org.assertj.core.api.Assertions.assertThat;

/** WS-US-01 / WS-US-02: sign-up starts a session and only the e-mailed code activates the account. */
public class RegistrationStepDefinitions extends AbstractCucumberSteps {

    @Dado("que un visitante inicia su registro con un correo nuevo y la contraseña {string}")
    public void iniciaRegistro(String password) {
        startRegistration(uniqueEmail("registro"), password);
    }

    @Cuando("confirma el registro con el código de verificación recibido")
    public void confirmaConCodigoRecibido() {
        confirmRegistration(capturedVerificationCode(state.email));
    }

    @Cuando("confirma el registro con el código {string}")
    public void confirmaConCodigo(String code) {
        confirmRegistration(code);
    }

    @Y("la cuenta queda activa y puede iniciar sesión con la contraseña {string}")
    public void cuentaActiva(String password) {
        assertThat(signIn(state.email, password).getStatusCode().value()).isEqualTo(200);
        assertThat(state.accessToken).isNotBlank();
    }

    @Y("la cuenta no queda creada")
    public void cuentaNoCreada() {
        assertThat(signIn(state.email, state.password).getStatusCode().value()).isEqualTo(401);
    }
}
