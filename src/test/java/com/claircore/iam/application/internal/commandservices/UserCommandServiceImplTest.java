package com.claircore.iam.application.internal.commandservices;

import com.claircore.iam.application.internal.outboundservices.acl.AsyncNotificationService;
import com.claircore.iam.domain.model.commands.ConfirmRegistrationCommand;
import com.claircore.iam.domain.model.commands.InitiateRegistrationCommand;
import com.claircore.iam.domain.model.aggregates.RegistrationSession;
import com.claircore.iam.domain.model.aggregates.User;
import com.claircore.iam.interfaces.events.UserRegisteredIntegrationEvent;
import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.OAuthProvider;
import com.claircore.iam.domain.model.valueobjects.Password;
import com.claircore.iam.domain.model.valueobjects.RegistrationSessionId;
import com.claircore.iam.domain.model.valueobjects.VerificationCode;
import com.claircore.iam.domain.repositories.UserRepository;
import com.claircore.iam.domain.repositories.RegistrationSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegistrationSessionRepository registrationSessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AsyncNotificationService asyncNotificationService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private UserCommandServiceImpl service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new UserCommandServiceImpl(
                userRepository,
                registrationSessionRepository,
                passwordEncoder,
                asyncNotificationService,
                eventPublisher
        );
    }

    @Test
    void shouldInitiateRegistrationWhenEmailDoesNotExist() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("encoded-password");

        var command = new InitiateRegistrationCommand("user@example.com", "SecurePass123!");

        var result = service.handle(command);

        assertTrue(result.isPresent());
        assertEquals("user@example.com", result.get().email().address());
        verify(registrationSessionRepository).save(any(RegistrationSession.class));
        verify(asyncNotificationService).sendVerificationCode("user@example.com", result.get().verificationCode().code());
    }

    @Test
    void shouldNotInitiateRegistrationWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail(any())).thenReturn(true);

        var result = service.handle(new InitiateRegistrationCommand("user@example.com", "SecurePass123!"));

        assertTrue(result.isEmpty());
        verify(registrationSessionRepository, never()).save(any());
        verify(asyncNotificationService, never()).sendVerificationCode(anyString(), anyString());
    }

    @Test
    void shouldConfirmRegistrationWhenVerificationCodeMatches() {
        RegistrationSession session = new RegistrationSession(
                RegistrationSessionId.generate(),
                new EmailAddress("user@example.com"),
                "encoded-password",
                new VerificationCode("6G13-789D"),
                30
        );

        when(registrationSessionRepository.findById(session.sessionId())).thenReturn(Optional.of(session));
        when(userRepository.existsByEmail(session.email())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return User.rehydrate(
                    UUID.randomUUID(),
                    user.getEmail(),
                    user.getPassword(),
                    com.claircore.iam.domain.model.valueobjects.UserStatus.ACTIVE,
                    user.getOauthProvider(),
                    user.getOauthSubject()
            ,
                null, null);
        });

        var result = service.handle(new ConfirmRegistrationCommand(session.sessionId(), "6G13-789D"));

        assertTrue(result.isPresent());
        assertEquals("user@example.com", result.get().getEmail().address());
        assertEquals(OAuthProvider.MAIL, result.get().getOauthProvider());
        assertEquals("encoded-password", result.get().getPassword().passwordHash());
        assertTrue(result.get().isActive());
        verify(registrationSessionRepository).deleteById(session.sessionId());
        verify(asyncNotificationService).sendWelcomeEmail("user@example.com");
        verify(eventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(UserRegisteredIntegrationEvent.class));
    }

    @Test
    void shouldDeleteExpiredSessionAndThrowWhenRegistrationSessionIsExpired() {
        RegistrationSession session = new RegistrationSession(
                RegistrationSessionId.generate(),
                new EmailAddress("user@example.com"),
                "encoded-password",
                new VerificationCode("6G13-789D"),
                java.time.Instant.now().minusSeconds(120),
                java.time.Instant.now().minusSeconds(60)
        );

        when(registrationSessionRepository.findById(session.sessionId())).thenReturn(Optional.of(session));

        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> service.handle(new ConfirmRegistrationCommand(session.sessionId(), "6G13-789D"))
        );

        assertEquals("Registration session has expired", exception.getMessage());
        verify(registrationSessionRepository).deleteById(session.sessionId());
        verify(userRepository, never()).save(any());
        verify(asyncNotificationService, never()).sendWelcomeEmail(anyString());
    }

    @Test
    void shouldThrowWhenVerificationCodeDoesNotMatch() {
        RegistrationSession session = new RegistrationSession(
                RegistrationSessionId.generate(),
                new EmailAddress("user@example.com"),
                "encoded-password",
                new VerificationCode("6G13-789D"),
                30
        );

        when(registrationSessionRepository.findById(session.sessionId())).thenReturn(Optional.of(session));

        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> service.handle(new ConfirmRegistrationCommand(session.sessionId(), "0000-0000"))
        );

        assertEquals("Invalid verification code", exception.getMessage());
        verify(registrationSessionRepository, never()).deleteById(any());
        verify(userRepository, never()).save(any());
    }
}
