package com.claircore.integration;

import com.claircore.billing.domain.model.valueobjects.PlanType;
import com.claircore.billing.domain.repositories.UserPlanRepository;
import com.claircore.iam.application.commandservices.UserCommandService;
import com.claircore.iam.domain.model.aggregates.RegistrationSession;
import com.claircore.iam.domain.model.aggregates.User;
import com.claircore.iam.domain.model.commands.ConfirmRegistrationCommand;
import com.claircore.iam.domain.model.commands.InitiateRegistrationCommand;
import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Integración IAM → Billing: confirmar el registro crea el plan Freemium del usuario")
class UserPlanIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserCommandService userCommandService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserPlanRepository userPlanRepository;

    @Test
    @DisplayName("Registrar y confirmar una cuenta persiste al usuario activo y Billing le asigna Freemium")
    void confirmingRegistrationInitializesAFreemiumPlan() {
        // Business / User Story Rational (WS-US-01, WS-US-02, WS-US-46): toda cuenta confirmada nace con el plan
        // Freemium, creado por Billing al recibir el evento de usuario registrado.
        // Arrange
        RegistrationSession session = userCommandService
                .handle(new InitiateRegistrationCommand("nuevo@clair.pe", "Clair#2026"))
                .orElseThrow();
        when(registrationSessionRepository.findById(session.sessionId())).thenReturn(Optional.of(session));

        // Act
        User user = userCommandService
                .handle(new ConfirmRegistrationCommand(session.sessionId(), session.verificationCode().code()))
                .orElseThrow();

        // Assert
        assertThat(userRepository.findByEmail(new EmailAddress("nuevo@clair.pe"))).get()
                .extracting(User::isActive).isEqualTo(true);
        var plan = userPlanRepository.findByUserId(new com.claircore.billing.domain.model.valueobjects.UserId(user.getId()));
        assertThat(plan).get().extracting(p -> p.getPlanType()).isEqualTo(PlanType.FREEMIUM);
        verify(asyncNotificationService).sendVerificationCode(eq("nuevo@clair.pe"), eq(session.verificationCode().code()));
    }

    @Test
    @DisplayName("Un código de verificación incorrecto no crea usuario ni plan")
    void aWrongCodeCreatesNeitherUserNorPlan() {
        // Business / User Story Rational (WS-US-02): sin el código correcto la cuenta no se persiste.
        // Arrange
        RegistrationSession session = userCommandService
                .handle(new InitiateRegistrationCommand("intruso@clair.pe", "Clair#2026"))
                .orElseThrow();
        when(registrationSessionRepository.findById(session.sessionId())).thenReturn(Optional.of(session));

        // Act + Assert
        assertThatThrownBy(() -> userCommandService.handle(new ConfirmRegistrationCommand(session.sessionId(), "ZZZZ-9999")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid verification code");
        assertThat(userRepository.existsByEmail(new EmailAddress("intruso@clair.pe"))).isFalse();
    }

    @Test
    @DisplayName("Un correo ya registrado no puede iniciar un segundo registro")
    void anExistingEmailCannotRegisterAgain() {
        // Business / User Story Rational (WS-US-01): un correo identifica a una sola cuenta.
        // Arrange
        RegistrationSession session = userCommandService
                .handle(new InitiateRegistrationCommand("repetido@clair.pe", "Clair#2026"))
                .orElseThrow();
        when(registrationSessionRepository.findById(session.sessionId())).thenReturn(Optional.of(session));
        userCommandService.handle(new ConfirmRegistrationCommand(session.sessionId(), session.verificationCode().code()));

        // Act
        Optional<RegistrationSession> second = userCommandService
                .handle(new InitiateRegistrationCommand("repetido@clair.pe", "OtraClave#1"));

        // Assert
        assertThat(second).isEmpty();
    }
}
