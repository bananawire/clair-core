package com.claircore.system;

import com.claircore.billing.application.internal.outboundservices.payments.PaymentGateway;
import com.claircore.device.application.commandservices.DeviceCommandService;
import com.claircore.device.domain.model.commands.ImportDevicesCommand;
import com.claircore.iam.application.internal.outboundservices.acl.ExternalNotificationService;
import com.claircore.iam.application.internal.outboundservices.oauth.GoogleTokenExchange;
import com.claircore.iam.application.internal.outboundservices.oauth.GoogleTokenVerifier;
import com.claircore.notifications.application.internal.outboundservices.email.EmailDeliveryService;
import com.claircore.notifications.application.internal.outboundservices.push.PushNotificationDeliveryService;
import com.claircore.testsupport.HermeticHttpTestConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * One ordered HTTP journey through the running application on H2: JWT security,
 * in-memory sessions and the IAM, Billing, Device, Evaluation, Alerting, Notifications and
 * Analytics contexts.
 * Each step reuses what the previous one produced, so the class must run in declared order.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@Import(HermeticHttpTestConfiguration.class)
@TestMethodOrder(OrderAnnotation.class)
@DisplayName("Clair Core – prueba de sistema del recorrido completo del usuario")
class ClairEndToEndSystemTest {

    private static final String PASSWORD = "Clair@2026";
    private static final String EMAIL = "e2e-" + UUID.randomUUID().toString().substring(0, 8) + "@clair.pe";

    private static UUID userId;
    private static String accessToken;
    private static String refreshToken;
    private static UUID organizationId;
    private static UUID spaceId;
    private static UUID deviceId;

    @MockitoBean
    PaymentGateway paymentGateway;

    @MockitoBean
    GoogleTokenVerifier googleTokenVerifier;

    @MockitoBean
    GoogleTokenExchange googleTokenExchange;

    @MockitoBean
    ExternalNotificationService externalNotificationService;

    @MockitoBean
    PushNotificationDeliveryService pushNotificationDeliveryService;

    @MockitoBean
    EmailDeliveryService emailDeliveryService;

    @Autowired
    TestRestTemplate rest;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    DeviceCommandService deviceCommandService;

    @Test
    @Order(1)
    @DisplayName("1. Registrarse y confirmar la cuenta con el código enviado por correo")
    void signUpAndConfirm() {
        // Business / User Story Rational (WS-US-01, WS-US-02): una cuenta solo existe cuando el correo fue verificado con el código enviado.
        // Arrange
        Map<String, String> signUp = Map.of("email", EMAIL, "password", PASSWORD);

        // Act
        ResponseEntity<String> initiated = call(HttpMethod.POST, "/api/v1/auth/sign-up", signUp, null);
        String sessionId = json(initiated).get("sessionId").asText();
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(externalNotificationService, timeout(5_000)).sendVerificationCode(eq(EMAIL), code.capture());
        ResponseEntity<String> confirmed = call(HttpMethod.POST, "/api/v1/auth/confirm",
                Map.of("sessionId", sessionId, "verificationCode", code.getValue()), null);

        // Assert
        assertThat(initiated.getStatusCode().value()).isEqualTo(201);
        assertThat(confirmed.getStatusCode().value()).isEqualTo(201);
        userId = UUID.fromString(json(confirmed).get("id").asText());
        assertThat(json(confirmed).get("email").asText()).isEqualTo(EMAIL);
    }

    @Test
    @Order(2)
    @DisplayName("2. Iniciar sesión obtiene el JWT y Billing asignó el plan Freemium")
    void signInAndReceiveFreemiumPlan() {
        // Business / User Story Rational (WS-US-03, WS-US-46): un usuario verificado accede con token y nace con el plan Freemium.
        // Arrange
        Map<String, String> credentials = Map.of("email", EMAIL, "password", PASSWORD);

        // Act
        ResponseEntity<String> signedIn = call(HttpMethod.POST, "/api/v1/auth/sign-in", credentials, null);
        accessToken = json(signedIn).get("token").asText();
        refreshToken = json(signedIn).get("refreshToken").asText();
        ResponseEntity<String> plan = call(HttpMethod.GET, "/api/v1/subscriptions/plans/" + userId, null, accessToken);

        // Assert
        assertThat(signedIn.getStatusCode().value()).isEqualTo(200);
        assertThat(accessToken).isNotBlank();
        assertThat(json(plan).get("plan").asText()).isEqualTo("freemium");
        Integer persistedPlans = jdbc.queryForObject(
                "SELECT count(*) FROM user_plan WHERE user_id = ? AND plan_type = 'FREEMIUM'", Integer.class, userId);
        assertThat(persistedPlans).isEqualTo(1);
    }

    @Test
    @Order(3)
    @DisplayName("3. Crear una organización y un espacio propios")
    void createOrganizationAndSpace() {
        // Business / User Story Rational (WS-US-24, WS-US-29): los sensores siempre se ubican en un espacio de una organización del usuario.
        // Arrange
        Map<String, String> organization = Map.of("name", "Oficinas Vanana");
        Map<String, String> space = Map.of("name", "Sala de reuniones");

        // Act
        ResponseEntity<String> createdOrganization = call(HttpMethod.POST, "/api/v1/organizations", organization, accessToken);
        organizationId = UUID.fromString(json(createdOrganization).get("id").asText());
        ResponseEntity<String> createdSpace = call(HttpMethod.POST,
                "/api/v1/spaces?organizationId=" + organizationId, space, accessToken);
        spaceId = UUID.fromString(json(createdSpace).get("id").asText());

        // Assert
        assertThat(createdOrganization.getStatusCode().value()).isEqualTo(201);
        assertThat(createdSpace.getStatusCode().value()).isEqualTo(201);
        assertThat(json(createdSpace).get("organizationId").asText()).isEqualTo(organizationId.toString());
    }

    @Test
    @Order(4)
    @DisplayName("4. Emparejar y reclamar un sensor Clair en el espacio")
    void pairAndClaimDevice() {
        // Business / User Story Rational (WS-US-10, WS-US-11): solo un sensor del inventario de fábrica puede emparejarse y reclamarse una vez.
        // Arrange
        String hardwareId = provisionFactoryDevice();

        // Act
        ResponseEntity<String> paired = call(HttpMethod.POST, "/api/v1/devices/pair",
                Map.of("hardwareId", hardwareId), accessToken);
        String claimToken = json(paired).get("claimToken").asText();
        ResponseEntity<String> claimed = call(HttpMethod.POST, "/api/v1/devices/claim",
                Map.of("claimToken", claimToken, "spaceId", spaceId.toString()), accessToken);
        deviceId = UUID.fromString(json(claimed).get("id").asText());

        // Assert
        assertThat(paired.getStatusCode().value()).isEqualTo(201);
        assertThat(claimed.getStatusCode().value()).isEqualTo(200);
        assertThat(json(claimed).get("spaceId").asText()).isEqualTo(spaceId.toString());
        assertThat(json(claimed).get("ownerUserId").asText()).isEqualTo(userId.toString());
    }

    @Test
    @Order(5)
    @DisplayName("5. Configurar el umbral de PM2.5 del dispositivo")
    void createPm25Threshold() {
        // Business / User Story Rational (WS-US-18): el usuario define a partir de qué valor una métrica es peligrosa.
        // Arrange
        Map<String, Object> threshold = Map.of("metric", "PM25", "value", new BigDecimal("50.00"), "enabled", true);

        // Act
        ResponseEntity<String> created = call(HttpMethod.POST,
                "/api/v1/devices/" + deviceId + "/thresholds", threshold, accessToken);

        // Assert
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        assertThat(json(created).get("metric").asText()).isEqualTo("PM25");
        assertThat(new BigDecimal(json(created).get("value").asText())).isEqualByComparingTo("50.00");
    }

    @Test
    @Order(6)
    @DisplayName("6. Registrar una lectura que supera el umbral notifica al dueño por push")
    void recordTelemetryAboveThreshold() {
        // Business / User Story Rational (WS-US-49, WS-US-52): toda lectura se evalúa contra sus umbrales y una superación avisa al dueño.
        // Arrange
        Map<String, Object> reading = telemetryReading(90.0);

        // Act
        ResponseEntity<String> recorded = call(HttpMethod.POST, "/api/v1/evaluations/telemetry", reading, null);

        // Assert
        assertThat(recorded.getStatusCode().value()).isEqualTo(201);
        verify(pushNotificationDeliveryService, timeout(5_000)).sendPushNotification(eq(userId), anyString(), anyString());
    }

    @Test
    @Order(7)
    @DisplayName("7. La lectura que supera el umbral aparece como alerta activa")
    void alertIsListed() {
        // Business / User Story Rational (WS-US-34): el usuario ve las alertas abiertas de todos sus dispositivos.
        // Arrange
        String path = "/api/v1/alerts?status=ACTIVE";

        // Act
        ResponseEntity<String> alerts = call(HttpMethod.GET, path, null, accessToken);

        // Assert
        assertThat(alerts.getStatusCode().value()).isEqualTo(200);
        JsonNode content = json(alerts).get("content");
        assertThat(content).hasSize(1);
        JsonNode alert = content.get(0);
        assertThat(alert.get("deviceId").asText()).isEqualTo(deviceId.toString());
        assertThat(alert.get("metric").asText()).isEqualTo("PM25");
        assertThat(alert.get("severity").asText()).isEqualTo("CRITICAL");
        assertThat(alert.get("spaceName").asText()).isEqualTo("Sala de reuniones");
    }

    @Test
    @Order(8)
    @DisplayName("8. El resumen de analíticas lista el espacio y el dispositivo")
    void overviewListsSpaceAndDevice() {
        // Business / User Story Rational (WS-US-43): el resumen general consolida organizaciones, espacios y dispositivos del usuario.
        // Arrange
        String path = "/api/v1/analytics/overview";

        // Act
        ResponseEntity<String> overview = call(HttpMethod.GET, path, null, accessToken);

        // Assert
        assertThat(overview.getStatusCode().value()).isEqualTo(200);
        JsonNode body = json(overview);
        assertThat(body.get("core").get("deviceCount").asInt()).isEqualTo(1);
        List<String> spaceIds = new ArrayList<>();
        body.get("organizations").forEach(org -> org.get("spaces").forEach(s -> spaceIds.add(s.get("spaceId").asText())));
        assertThat(spaceIds).containsExactly(spaceId.toString());
    }

    @Test
    @Order(9)
    @DisplayName("9. Cerrar sesión revoca el refresh token")
    void signOutRevokesRefreshToken() {
        // Business / User Story Rational (WS-US-07, WS-US-08): tras cerrar sesión ningún token anterior puede renovar el acceso.
        // Arrange
        Map<String, String> refresh = Map.of("refreshToken", refreshToken);

        // Act
        ResponseEntity<String> signedOut = call(HttpMethod.DELETE, "/api/v1/auth/sign-out", null, accessToken);
        ResponseEntity<String> refreshed = call(HttpMethod.POST, "/api/v1/auth/refresh", refresh, null);

        // Assert
        assertThat(signedOut.getStatusCode().value()).isEqualTo(204);
        assertThat(refreshed.getStatusCode().value()).isEqualTo(401);
    }

    private ResponseEntity<String> call(HttpMethod method, String path, Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return rest.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }

    private JsonNode json(ResponseEntity<String> response) {
        try {
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new AssertionError("Response body is not JSON: " + response.getBody(), e);
        }
    }

    /** The journey provisions its own unclaimed sensor through the device command service. */
    private String provisionFactoryDevice() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
        String hardwareId = "CLAIR-" + suffix;
        String unique = UUID.randomUUID().toString();
        deviceCommandService.handle(new ImportDevicesCommand(List.of(
                new ImportDevicesCommand.DeviceProvisioningRecord(
                        "SN-" + unique, hardwareId, "key-" + unique, "Sensor " + suffix))));
        return hardwareId;
    }

    private Map<String, Object> telemetryReading(double pm25) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("deviceId", deviceId.toString());
        body.put("readingId", UUID.randomUUID().toString());
        body.put("uptime", "20");
        body.put("airQuality", Map.of("co2", 480.0, "temperature", 23.5, "humidity", 52.0));
        body.put("particulateMatter", Map.of("pm1_0", 5.0, "pm2_5", pm25, "pm10", 25.0));
        body.put("connectivity", Map.of("status", "connected", "network", "Clair-Lab", "signalStrength", -60));
        body.put("location", Map.of("country", "PERU"));
        body.put("healthStatus", 100);
        body.put("status", "Optimal");
        body.put("measuredAt", Instant.now().truncatedTo(ChronoUnit.SECONDS).plusSeconds(1).toString());
        return body;
    }
}
