package com.claircore.iam.application.internal.commandservices;

import com.claircore.iam.domain.model.commands.SignOutCommand;
import com.claircore.iam.application.commandservices.TokenCommandService;
import com.claircore.iam.application.internal.outboundservices.tokens.TokenService;
import com.claircore.iam.domain.model.aggregates.TokenSession;
import com.claircore.iam.domain.model.aggregates.User;
import com.claircore.iam.domain.model.valueobjects.TokenJti;
import com.claircore.iam.domain.model.valueobjects.TokenType;
import com.claircore.iam.domain.repositories.TokenSessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class TokenCommandServiceImpl implements TokenCommandService {

    private final TokenService tokenService;
    private final TokenSessionRepository tokenSessionRepository;
    private final long accessTtlMillis;
    private final long refreshTtlMillis;

    public TokenCommandServiceImpl(
            TokenService tokenService,
            TokenSessionRepository tokenSessionRepository,
            @Value("${jwt.expiration}") long accessTtlMillis,
            @Value("${jwt.refresh-expiration}") long refreshTtlMillis
    ) {
        this.tokenService = tokenService;
        this.tokenSessionRepository = tokenSessionRepository;
        this.accessTtlMillis = accessTtlMillis;
        this.refreshTtlMillis = refreshTtlMillis;
    }

    @Override
    @Transactional
    public String createAccessToken(User user) {
        TokenJti jti = TokenJti.generate();
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(accessTtlMillis);

        TokenSession session = new TokenSession(jti, user.getEmail(), user.getId(), TokenType.ACCESS, now, expiresAt);
        tokenSessionRepository.replaceForUser(session);

        return tokenService.generateAccessToken(user.getId(), accessTtlMillis, jti.jti());
    }

    @Override
    @Transactional
    public String createRefreshToken(User user) {
        TokenJti jti = TokenJti.generate();
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(refreshTtlMillis);

        TokenSession session = new TokenSession(jti, user.getEmail(), user.getId(), TokenType.REFRESH, now, expiresAt);
        tokenSessionRepository.replaceForUser(session);

        return tokenService.generateRefreshToken(user.getId(), refreshTtlMillis, jti.jti());
    }

    @Override
    @Transactional
    public void invalidateAccessToken(String jwtToken) {
        tokenService.extractJti(jwtToken)
                .ifPresent(jti -> tokenSessionRepository.deleteByJti(new TokenJti(jti), TokenType.ACCESS));
    }

    @Override
    @Transactional
    public void invalidateRefreshToken(String jwtToken) {
        tokenService.extractJti(jwtToken)
                .ifPresent(jti -> tokenSessionRepository.deleteByJti(new TokenJti(jti), TokenType.REFRESH));
    }

    @Override
    @Transactional
    public Optional<String> rotateRefreshToken(String refreshTokenJwt) {
        Optional<String> jtiOpt = tokenService.extractJti(refreshTokenJwt);
        Optional<String> typeOpt = tokenService.extractType(refreshTokenJwt);

        if (jtiOpt.isEmpty() || typeOpt.isEmpty() || !"refresh".equals(typeOpt.get())) {
            return Optional.empty();
        }

        TokenJti oldJti = new TokenJti(jtiOpt.get());
        Optional<TokenSession> existingSession = tokenSessionRepository.findByJti(oldJti, TokenType.REFRESH);
        if (existingSession.isEmpty()) {
            return Optional.empty();
        }

        tokenSessionRepository.deleteByJti(oldJti, TokenType.REFRESH);

        TokenJti newJti = TokenJti.generate();
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(refreshTtlMillis);
        TokenSession newSession = new TokenSession(newJti, existingSession.get().email(), existingSession.get().userId(), TokenType.REFRESH, now, expiresAt);
        tokenSessionRepository.replaceForUser(newSession);

        return Optional.of(tokenService.generateRefreshToken(existingSession.get().userId(), refreshTtlMillis, newJti.jti()));
    }

    @Override
    @Transactional
    public void signOut(SignOutCommand command) {
        tokenSessionRepository.revokeAllTokensForUser(command.userId().userId());
    }
}
