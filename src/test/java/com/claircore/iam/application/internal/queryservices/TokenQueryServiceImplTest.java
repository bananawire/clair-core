package com.claircore.iam.application.internal.queryservices;

import com.claircore.iam.domain.model.aggregates.TokenSession;
import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.TokenJti;
import com.claircore.iam.domain.model.valueobjects.TokenType;
import com.claircore.iam.domain.repositories.TokenSessionRepository;
import com.claircore.iam.infrastructure.tokens.jwt.JwtTokenEncoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenQueryServiceImplTest {

    @Mock
    private TokenSessionRepository tokenSessionRepository;

    @Mock
    private JwtTokenEncoder jwtTokenEncoder;

    @InjectMocks
    private TokenQueryServiceImpl service;

    @Test
    void shouldReturnTrueWhenAccessTokenIsValidAndStored() {
        TokenJti jti = new TokenJti(UUID.randomUUID().toString());
        when(jwtTokenEncoder.extractJti("token")).thenReturn(Optional.of(jti.jti()));
        when(jwtTokenEncoder.extractType("token")).thenReturn(Optional.of("access"));
        when(tokenSessionRepository.existsByJti(jti, TokenType.ACCESS)).thenReturn(true);

        assertTrue(service.isAccessTokenValid("token"));
    }

    @Test
    void shouldReturnFalseWhenTokenTypeDoesNotMatchExpectedType() {
        when(jwtTokenEncoder.extractJti("token")).thenReturn(Optional.of(UUID.randomUUID().toString()));
        when(jwtTokenEncoder.extractType("token")).thenReturn(Optional.of("refresh"));

        assertFalse(service.isAccessTokenValid("token"));
    }

    @Test
    void shouldReturnTokenSessionWhenTokenIsStored() {
        TokenJti jti = new TokenJti(UUID.randomUUID().toString());
        TokenSession session = new TokenSession(
                jti,
                new EmailAddress("user@example.com"),
                UUID.randomUUID(),
                TokenType.REFRESH,
                Instant.now(),
                Instant.now().plusSeconds(60)
        );

        when(jwtTokenEncoder.extractJti("token")).thenReturn(Optional.of(jti.jti()));
        when(jwtTokenEncoder.extractType("token")).thenReturn(Optional.of("refresh"));
        when(tokenSessionRepository.findByJti(jti, TokenType.REFRESH)).thenReturn(Optional.of(session));

        Optional<TokenSession> result = service.getTokenSession("token");

        assertTrue(result.isPresent());
        assertEquals(session, result.get());
    }

    @Test
    void shouldReturnEmptyUserIdWhenTokenCannotBeParsed() {
        when(jwtTokenEncoder.extractUserId("invalid-token")).thenReturn(Optional.empty());

        assertTrue(service.getUserIdFromToken("invalid-token").isEmpty());
    }
}
