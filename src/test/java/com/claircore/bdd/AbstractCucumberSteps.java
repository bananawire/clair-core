package com.claircore.bdd;

import com.claircore.iam.application.internal.outboundservices.acl.ExternalNotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Shared HTTP plumbing for every step definition class. Scenario state is static because Cucumber
 * builds one glue instance per class per scenario, while a scenario spans several glue classes
 * (for example the common "la respuesta tiene estado" step). It is reset before every scenario.
 */
public abstract class AbstractCucumberSteps {

    private static final String HARDWARE_ID_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    protected static ScenarioState state = new ScenarioState();

    @Autowired
    protected TestRestTemplate rest;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected ExternalNotificationService notifications;

    static void resetState() {
        state = new ScenarioState();
    }

    protected static final class ScenarioState {
        String email;
        String password;
        String registrationSessionId;
        UUID userId;
        String accessToken;
        String refreshToken;
        UUID organizationId;
        UUID spaceId;
        UUID deviceId;
        ResponseEntity<String> lastResponse;
    }

    // ---------------------------------------------------------------- HTTP

    protected ResponseEntity<String> send(HttpMethod method, String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        if (state.accessToken != null) {
            headers.setBearerAuth(state.accessToken);
        }
        ResponseEntity<String> response = rest.exchange(path, method, new HttpEntity<>(body, headers), String.class);
        state.lastResponse = response;
        return response;
    }

    protected ResponseEntity<String> post(String path, Object body) {
        return send(HttpMethod.POST, path, body);
    }

    protected ResponseEntity<String> put(String path, Object body) {
        return send(HttpMethod.PUT, path, body);
    }

    protected ResponseEntity<String> get(String path) {
        return send(HttpMethod.GET, path, null);
    }

    protected JsonNode json(ResponseEntity<String> response) {
        try {
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new AssertionError("Response body is not JSON: " + response.getBody(), e);
        }
    }

    // ---------------------------------------------------------------- IAM

    protected String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@clair.pe";
    }

    protected ResponseEntity<String> startRegistration(String email, String password) {
        state.email = email;
        state.password = password;
        ResponseEntity<String> response = post("/api/v1/auth/sign-up", Map.of("email", email, "password", password));
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        state.registrationSessionId = json(response).get("sessionId").asText();
        return response;
    }

    protected String capturedVerificationCode(String email) {
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(notifications, timeout(5_000)).sendVerificationCode(eq(email), code.capture());
        return code.getValue();
    }

    protected ResponseEntity<String> confirmRegistration(String code) {
        ResponseEntity<String> response = post("/api/v1/auth/confirm",
                Map.of("sessionId", state.registrationSessionId, "verificationCode", code));
        if (response.getStatusCode().value() == 201) {
            state.userId = UUID.fromString(json(response).get("id").asText());
        }
        return response;
    }

    protected ResponseEntity<String> signIn(String email, String password) {
        state.accessToken = null;
        ResponseEntity<String> response = post("/api/v1/auth/sign-in", Map.of("email", email, "password", password));
        if (response.getStatusCode().value() == 200) {
            JsonNode body = json(response);
            state.accessToken = body.get("token").asText();
            state.refreshToken = body.get("refreshToken").asText();
            state.userId = UUID.fromString(body.get("id").asText());
        }
        return response;
    }

    /** Registers (sign-up + confirm with the captured code) and signs in a brand-new verified user. */
    protected void registerAndSignIn(String email, String password) {
        startRegistration(email, password);
        ResponseEntity<String> confirmed = confirmRegistration(capturedVerificationCode(email));
        assertThat(confirmed.getStatusCode().value()).isEqualTo(201);
        assertThat(signIn(email, password).getStatusCode().value()).isEqualTo(200);
    }

    // ---------------------------------------------------------------- Billing

    /** Plans are only upgraded through Stripe, which is mocked; the test data is arranged directly. */
    protected void assignPlan(UUID userId, String planType) {
        int updated = jdbc.update(
                "UPDATE user_plan SET plan_type = ?, end_date = CURRENT_DATE + 30, updated_at = now() WHERE user_id = ?",
                planType, userId);
        if (updated == 0) {
            jdbc.update("INSERT INTO user_plan (id, user_id, plan_type, start_date, end_date, created_at, updated_at) "
                    + "VALUES (?, ?, ?, CURRENT_DATE, CURRENT_DATE + 30, now(), now())",
                    UUID.randomUUID(), userId, planType);
        }
    }

    // ---------------------------------------------------------------- Device

    protected UUID createOrganization(String name) {
        ResponseEntity<String> response = post("/api/v1/organizations", Map.of("name", name));
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        state.organizationId = UUID.fromString(json(response).get("id").asText());
        return state.organizationId;
    }

    protected ResponseEntity<String> createSpace(UUID organizationId, String name) {
        ResponseEntity<String> response = post("/api/v1/spaces?organizationId=" + organizationId, Map.of("name", name));
        if (response.getStatusCode().value() == 201) {
            state.spaceId = UUID.fromString(json(response).get("id").asText());
        }
        return response;
    }

    /** Factory inventory is fixed by seeds; each scenario provisions its own unclaimed sensor. */
    protected String provisionFactoryDevice() {
        StringBuilder suffix = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            suffix.append(HARDWARE_ID_ALPHABET.charAt(RANDOM.nextInt(HARDWARE_ID_ALPHABET.length())));
        }
        String hardwareId = "CLAIR-" + suffix;
        String unique = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO devices (id, serial_number, hardware_id, api_key, name, factory_name, device_type, "
                        + "deleted, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, 'air-quality-v1', false, now(), now())",
                UUID.randomUUID(), "SN-" + unique, hardwareId, "key-" + unique, "Sensor " + suffix, "Sensor " + suffix);
        return hardwareId;
    }

    /** Pair + claim a freshly provisioned sensor into the scenario's space. */
    protected UUID pairAndClaimDevice() {
        String hardwareId = provisionFactoryDevice();
        ResponseEntity<String> paired = post("/api/v1/devices/pair", Map.of("hardwareId", hardwareId));
        assertThat(paired.getStatusCode().value()).isEqualTo(201);
        String claimToken = json(paired).get("claimToken").asText();

        ResponseEntity<String> claimed = post("/api/v1/devices/claim",
                Map.of("claimToken", claimToken, "spaceId", state.spaceId.toString()));
        assertThat(claimed.getStatusCode().value()).isEqualTo(200);
        state.deviceId = UUID.fromString(json(claimed).get("id").asText());
        return state.deviceId;
    }

    /** New verified user with one organization, one space and one claimed device. */
    protected void userWithClaimedDevice() {
        registerAndSignIn(uniqueEmail("sensor"), "Clair@2026");
        UUID organizationId = createOrganization("Hogar Clair");
        assertThat(createSpace(organizationId, "Sala").getStatusCode().value()).isEqualTo(201);
        pairAndClaimDevice();
    }
}
