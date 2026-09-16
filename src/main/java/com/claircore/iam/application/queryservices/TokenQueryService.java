package com.claircore.iam.application.queryservices;

import com.claircore.iam.domain.model.aggregates.TokenSession;

import java.util.Optional;
import java.util.UUID;

public interface TokenQueryService {
    boolean isAccessTokenValid(String jwtToken);
    boolean isRefreshTokenValid(String jwtToken);
    Optional<UUID> getUserIdFromToken(String jwtToken);
    Optional<TokenSession> getTokenSession(String jwtToken);
}
