package com.claircore.iam.infrastructure.oauth.google;

import com.claircore.iam.domain.model.valueobjects.*;
import com.claircore.iam.application.internal.outboundservices.oauth.GoogleTokenVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class GoogleTokenVerifierImpl implements GoogleTokenVerifier {

    private static final String TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo";
    private static final String ISSUER_GOOGLE = "https://accounts.google.com";
    private static final String ISSUER_GOOGLE_SHORT = "accounts.google.com";

    private RestTemplate restTemplate;
    private final Set<String> allowedClientIds;

    public GoogleTokenVerifierImpl(
            @Value("${google.oauth.client-id}") String primaryClientId,
            @Value("${google.oauth.allowed-client-ids:}") String allowedClientIds
    ) {
        this.restTemplate = new RestTemplate();
        final String effectiveAllowed = (allowedClientIds == null || allowedClientIds.isBlank())
                ? primaryClientId
                : allowedClientIds;

        this.allowedClientIds = effectiveAllowed == null
                ? Set.of()
                : Set.of(effectiveAllowed.split(",")).stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Optional<VerifiedGoogleIdentity> verify(GoogleIdToken idToken) {
        try {
            if (allowedClientIds.isEmpty()) {
                throw new IllegalStateException("Google OAuth client ID is not configured");
            }

            String url = UriComponentsBuilder.fromHttpUrl(TOKENINFO_URL)
                    .queryParam("id_token", idToken.token())
                    .toUriString();

            @SuppressWarnings("unchecked")
            Map<String, Object> payload = restTemplate.getForObject(url, Map.class);

            if (payload == null) {
                return Optional.empty();
            }

            String iss = (String) payload.get("iss");
            if (!ISSUER_GOOGLE.equals(iss) && !ISSUER_GOOGLE_SHORT.equals(iss)) {
                return Optional.empty();
            }

            String aud = (String) payload.get("aud");
            if (aud == null || aud.isBlank() || !allowedClientIds.contains(aud)) {
                return Optional.empty();
            }

            Boolean emailVerified = Boolean.valueOf(String.valueOf(payload.get("email_verified")));
            if (!emailVerified) {
                return Optional.empty();
            }

            String expStr = (String) payload.get("exp");
            if (expStr != null && !expStr.isBlank()) {
                long exp = Long.parseLong(expStr);
                if (System.currentTimeMillis() / 1000 > exp) {
                    return Optional.empty();
                }
            }

            String email = (String) payload.get("email");
            String sub = (String) payload.get("sub");

            if (email == null || email.isBlank() || sub == null || sub.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(new VerifiedGoogleIdentity(
                    new EmailAddress(email),
                    new GoogleUserId(sub),
                    true
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
