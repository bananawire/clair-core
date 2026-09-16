package com.claircore.iam.infrastructure.oauth.google;

import com.claircore.iam.application.internal.outboundservices.oauth.OAuthStateService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class GoogleOAuthStateManager implements OAuthStateService {

    private static final String CLAIM_PURPOSE = "purpose";
    private static final String PURPOSE_VALUE = "oauth_state";
    private static final long STATE_TTL_MILLIS = 5 * 60 * 1000; // 5 minutes

    private final SecretKey secretKey;

    public GoogleOAuthStateManager(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generateState() {
        return Jwts.builder()
                .claim(CLAIM_PURPOSE, PURPOSE_VALUE)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + STATE_TTL_MILLIS))
                .signWith(secretKey)
                .compact();
    }

    @Override
    public boolean validateState(String state) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(state)
                    .getPayload();
            return PURPOSE_VALUE.equals(claims.get(CLAIM_PURPOSE, String.class));
        } catch (Exception e) {
            return false;
        }
    }
}
