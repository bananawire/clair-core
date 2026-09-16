package com.claircore.iam.application.internal.queryservices;

import com.claircore.iam.application.internal.outboundservices.tokens.TokenService;
import com.claircore.iam.application.queryservices.TokenQueryService;
import com.claircore.iam.domain.model.aggregates.TokenSession;
import com.claircore.iam.domain.model.valueobjects.TokenJti;
import com.claircore.iam.domain.model.valueobjects.TokenType;
import com.claircore.iam.domain.repositories.TokenSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class TokenQueryServiceImpl implements TokenQueryService {

    private final TokenSessionRepository tokenSessionRepository;
    private final TokenService tokenService;

    public TokenQueryServiceImpl(TokenSessionRepository tokenSessionRepository, TokenService tokenService) {
        this.tokenSessionRepository = tokenSessionRepository;
        this.tokenService = tokenService;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAccessTokenValid(String jwtToken) {
        return isTokenValidByType(jwtToken, TokenType.ACCESS);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRefreshTokenValid(String jwtToken) {
        return isTokenValidByType(jwtToken, TokenType.REFRESH);
    }

    private boolean isTokenValidByType(String jwtToken, TokenType expectedType) {
        Optional<String> jti = tokenService.extractJti(jwtToken);
        Optional<String> type = tokenService.extractType(jwtToken);

        if (jti.isEmpty() || type.isEmpty()) {
            return false;
        }

        if (!expectedType.name().toLowerCase().equals(type.get())) {
            return false;
        }

        return tokenSessionRepository.existsByJti(new TokenJti(jti.get()), expectedType);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> getUserIdFromToken(String jwtToken) {
        return tokenService.extractUserId(jwtToken);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TokenSession> getTokenSession(String jwtToken) {
        Optional<String> jti = tokenService.extractJti(jwtToken);
        Optional<String> type = tokenService.extractType(jwtToken);

        if (jti.isEmpty() || type.isEmpty()) {
            return Optional.empty();
        }

        TokenType tokenType = switch (type.get()) {
            case "access" -> TokenType.ACCESS;
            case "refresh" -> TokenType.REFRESH;
            default -> null;
        };

        if (tokenType == null) {
            return Optional.empty();
        }

        return tokenSessionRepository.findByJti(new TokenJti(jti.get()), tokenType);
    }
}
