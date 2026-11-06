package com.claircore.iam.unit;

import com.claircore.iam.domain.model.aggregates.RegistrationSession;
import com.claircore.iam.domain.model.aggregates.User;
import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.OAuthProvider;
import com.claircore.iam.domain.model.valueobjects.Password;
import com.claircore.iam.domain.model.valueobjects.RegistrationSessionId;
import com.claircore.iam.domain.model.valueobjects.UserStatus;
import com.claircore.iam.domain.model.valueobjects.VerificationCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("IAM – pruebas unitarias de User y RegistrationSession")
class IamUnitTest {

    private static final EmailAddress EMAIL = new EmailAddress("ana@clair.pe");

    private static RegistrationSession sessionExpiringAt(Instant expiresAt) {
        return new RegistrationSession(RegistrationSessionId.generate(), EMAIL, "hash",
                new VerificationCode("AB12-CD34"), expiresAt.minusSeconds(600), expiresAt);
    }

    @Test
    @DisplayName("Happy path: confirmar el registro activa al usuario")
    void confirmingRegistrationActivatesTheUser() {
        // Business / User Story Rational (WS-US-02): la cuenta solo se habilita al confirmar el código de verificación.
        // Arrange
        User user = new User(EMAIL, new Password("hash"));

        // Act
        user.activate();

        // Assert
        assertThat(user.isActive()).isTrue();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("Límite superior: un código válido justo antes de expirar todavía se acepta")
    void codeIsAcceptedJustBeforeExpiry() {
        // Business / User Story Rational (WS-US-02): el código vale durante toda su ventana de vigencia.
        // Arrange
        RegistrationSession session = sessionExpiringAt(Instant.now().plusSeconds(5));

        // Act
        boolean verified = session.verifyCode("AB12-CD34");

        // Assert
        assertThat(verified).isTrue();
    }

    @Test
    @DisplayName("Límite inferior: un código correcto pero ya expirado es rechazado")
    void expiredCodeIsRejected() {
        // Business / User Story Rational (WS-US-02): un código vencido no debe activar la cuenta.
        // Arrange
        RegistrationSession session = sessionExpiringAt(Instant.now().minusSeconds(1));

        // Act
        boolean verified = session.verifyCode("AB12-CD34");

        // Assert
        assertThat(session.isExpired()).isTrue();
        assertThat(verified).isFalse();
    }

    @Test
    @DisplayName("Datos insuficientes: un correo vacío no permite crear la cuenta")
    void blankEmailIsRejected() {
        // Business / User Story Rational (WS-US-01): el correo es el identificador de la cuenta.
        // Arrange
        String blankEmail = "  ";

        // Act + Assert
        assertThatThrownBy(() -> new EmailAddress(blankEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email address cannot be null or empty");
    }

    @Test
    @DisplayName("Datos insuficientes: un código de verificación con formato inválido es rechazado")
    void malformedVerificationCodeIsRejected() {
        // Business / User Story Rational (WS-US-02): un code incorrecto no permite confirmar el registro.
        // Arrange
        String lowercaseCode = "ab12-cd34";

        // Act + Assert
        assertThatThrownBy(() -> new VerificationCode(lowercaseCode))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Verification code must be in format XXXX-XXXX (uppercase alphanumeric)");
    }

    @Test
    @DisplayName("Estado inválido: un usuario registrado con correo queda pendiente de verificación")
    void mailUserStartsPendingVerification() {
        // Business / User Story Rational (WS-US-03): un usuario no verificado no debe poder iniciar sesión.
        // Arrange + Act
        User user = new User(EMAIL, new Password("hash"));

        // Assert
        assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        assertThat(user.isActive()).isFalse();
    }

    @Test
    @DisplayName("Condicional A: un usuario creado con correo y contraseña no es usuario OAuth")
    void mailUserIsNotOAuth() {
        // Business / User Story Rational (WS-US-03): el inicio de sesión con contraseña aplica a cuentas MAIL.
        // Arrange + Act
        User user = new User(EMAIL, new Password("hash"));

        // Assert
        assertThat(user.getOauthProvider()).isEqualTo(OAuthProvider.MAIL);
        assertThat(user.isOAuthUser()).isFalse();
    }

    @Test
    @DisplayName("Condicional B: un usuario creado con Google nace activo y es usuario OAuth")
    void googleUserIsActiveAndOAuth() {
        // Business / User Story Rational (WS-US-04): Google ya verificó el correo, así que la cuenta queda activa.
        // Arrange + Act
        User user = new User(EMAIL, OAuthProvider.GOOGLE, "google-subject-123");

        // Assert
        assertThat(user.isActive()).isTrue();
        assertThat(user.isOAuthUser()).isTrue();
        assertThat(user.getOauthSubject()).isEqualTo("google-subject-123");
    }

    @Test
    @DisplayName("Integridad: un código distinto al enviado no verifica la sesión")
    void wrongCodeDoesNotVerify() {
        // Business / User Story Rational (WS-US-02): solo el dueño del correo conoce el código correcto.
        // Arrange
        RegistrationSession session = sessionExpiringAt(Instant.now().plusSeconds(600));

        // Act
        boolean verified = session.verifyCode("ZZ99-ZZ99");

        // Assert
        assertThat(verified).isFalse();
    }

    @Test
    @DisplayName("Integridad: vincular Google a una cuenta existente conserva su correo e identidad")
    void linkingGoogleKeepsEmailAndId() {
        // Business / User Story Rational (WS-US-04): vincular Google no debe crear una cuenta duplicada.
        // Arrange
        User user = new User(EMAIL, new Password("hash"));
        var originalId = user.getId();

        // Act
        user.linkOAuthAccount(OAuthProvider.GOOGLE, "google-subject-123");

        // Assert
        assertThat(user.getId()).isEqualTo(originalId);
        assertThat(user.getEmail()).isEqualTo(EMAIL);
        assertThat(user.isOAuthUser()).isTrue();
    }
}
