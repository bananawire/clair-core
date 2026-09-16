package com.claircore.iam.infrastructure.oauth.google;

import com.claircore.iam.application.internal.outboundservices.oauth.GoogleTokenExchange;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Component
public class GoogleAuthorizationCodeTokenClient implements GoogleTokenExchange {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";

    private RestTemplate restTemplate;

    public GoogleAuthorizationCodeTokenClient() {
        this.restTemplate = new RestTemplate();
    }

    void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Optional<String> exchangeCodeForIdToken(String code, String clientId, String clientSecret, String redirectUri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(TOKEN_URL, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.ofNullable((String) response.getBody().get("id_token"));
            }
        } catch (Exception e) {
            // Fail securely: do not propagate external details
        }

        return Optional.empty();
    }
}
