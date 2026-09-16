package com.claircore.iam.application.internal.commandservices;

import com.claircore.iam.domain.model.commands.SignOutCommand;
import com.claircore.iam.domain.model.aggregates.TokenSession;
import com.claircore.iam.domain.model.aggregates.User;
import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.Password;
import com.claircore.iam.domain.model.valueobjects.TokenJti;
import com.claircore.iam.domain.model.valueobjects.TokenType;
import com.claircore.iam.domain.repositories.TokenSessionRepository;
import com.claircore.iam.application.internal.outboundservices.tokens.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenCommandServiceImplTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private TokenSessionRepository tokenSessionRepository;

    private TokenCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TokenCommandServiceImpl(tokenService, tokenSessionRepository, 60000L, 120000L);
    }

    @Test
    void shouldCreateAccessTokenAndReplaceSessionWhenUserIsValid() {
        User user = userWithId("user@example.com");
        when(tokenService.generateAccessToken(org.mockito.ArgumentMatchers.any(java.util.UUID.class), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("access-token");

        String token = service.createAccessToken(user);

        assertEquals("access-token", token);
        ArgumentCaptor<TokenSession> captor = ArgumentCaptor.forClass(TokenSession.class);
        verify(tokenSessionRepository).replaceForUser(captor.capture());
        assertEquals(TokenType.ACCESS, captor.getValue().type());
        assertEquals(user.getId(), captor.getValue().userId());
        assertEquals(user.getEmail(), captor.getValue().email());
    }

    @Test
    void shouldCreateRefreshTokenAndReplaceSessionWhenUserIsValid() {
        User user = userWithId("user@example.com");
        when(tokenService.generateRefreshToken(org.mockito.ArgumentMatchers.any(java.util.UUID.class), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("refresh-token");

        String token = service.createRefreshToken(user);

        assertEquals("refresh-token", token);
        ArgumentCaptor<TokenSession> captor = ArgumentCaptor.forClass(TokenSession.class);
        verify(tokenSessionRepository).replaceForUser(captor.capture());
        assertEquals(TokenType.REFRESH, captor.getValue().type());
        assertEquals(user.getId(), captor.getValue().userId());
    }

    @Test
    void shouldInvalidateAccessTokenWhenJtiCanBeExtracted() {
        when(tokenService.extractJti("token")).thenReturn(Optional.of(UUID.randomUUID().toString()));

        service.invalidateAccessToken("token");

        verify(tokenSessionRepository).deleteByJti(any(TokenJti.class), org.mockito.ArgumentMatchers.eq(TokenType.ACCESS));
    }

    @Test
    void shouldReturnEmptyWhenRefreshTokenIsNotRefreshType() {
        when(tokenService.extractJti("token")).thenReturn(Optional.of(UUID.randomUUID().toString()));
        when(tokenService.extractType("token")).thenReturn(Optional.of("access"));

        Optional<String> rotated = service.rotateRefreshToken("token");

        assertTrue(rotated.isEmpty());
        verify(tokenSessionRepository, never()).findByJti(org.mockito.ArgumentMatchers.any(TokenJti.class), org.mockito.ArgumentMatchers.any(TokenType.class));
    }

    @Test
    void shouldRotateRefreshTokenWhenExistingSessionIsValid() {
        User user = userWithId("user@example.com");
        TokenJti oldJti = new TokenJti(UUID.randomUUID().toString());
        TokenSession existingSession = new TokenSession(
                oldJti,
                user.getEmail(),
                user.getId(),
                TokenType.REFRESH,
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(60)
        );

        when(tokenService.extractJti("refresh-token")).thenReturn(Optional.of(oldJti.jti()));
        when(tokenService.extractType("refresh-token")).thenReturn(Optional.of("refresh"));
        when(tokenSessionRepository.findByJti(oldJti, TokenType.REFRESH)).thenReturn(Optional.of(existingSession));
        when(tokenService.generateRefreshToken(org.mockito.ArgumentMatchers.any(java.util.UUID.class), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("rotated-refresh-token");

        Optional<String> rotated = service.rotateRefreshToken("refresh-token");

        assertTrue(rotated.isPresent());
        assertEquals("rotated-refresh-token", rotated.get());
        verify(tokenSessionRepository).deleteByJti(oldJti, TokenType.REFRESH);
        verify(tokenSessionRepository).replaceForUser(any(TokenSession.class));
    }

    @Test
    void shouldRevokeAllTokensWhenUserSignsOut() {
        UUID userId = UUID.randomUUID();

        service.signOut(new SignOutCommand(new com.claircore.iam.domain.model.valueobjects.UserId(userId)));

        verify(tokenSessionRepository).revokeAllTokensForUser(userId);
    }

    private User userWithId(String email) {
        return User.rehydrate(
                UUID.randomUUID(),
                new EmailAddress(email),
                new Password("encoded-password"),
                com.claircore.iam.domain.model.valueobjects.UserStatus.ACTIVE,
                com.claircore.iam.domain.model.valueobjects.OAuthProvider.MAIL,
                null
        ,
                null, null);
    }
}
