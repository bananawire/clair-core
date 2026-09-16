package com.claircore.notifications.infrastructure.communication.onesignal;

import com.claircore.notifications.application.internal.outboundservices.push.PushNotificationDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OneSignalPushNotificationService implements PushNotificationDeliveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OneSignalPushNotificationService.class);

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String appId;
    private final String restApiKey;
    private final String frontendUrl;

    public OneSignalPushNotificationService(
            @Value("${onesignal.api-url}") String apiUrl,
            @Value("${onesignal.app-id}") String appId,
            @Value("${onesignal.rest-api-key}") String restApiKey,
            @Value("${onesignal.frontend-url}") String frontendUrl
    ) {
        this.restTemplate = new RestTemplate();
        this.apiUrl = apiUrl;
        this.appId = appId;
        this.restApiKey = restApiKey;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void sendPushNotification(UUID userId, String title, String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + restApiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("app_id", appId);
        body.put("include_external_user_ids", List.of(userId.toString()));
        body.put("contents", Map.of("en", message));
        body.put("headings", Map.of("en", title));
        body.put("url", frontendUrl);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                LOGGER.info("OneSignal push notification response: {}", response.getBody());
            } else {
                throw new RuntimeException("OneSignal returned status code " + response.getStatusCode() + ": " + response.getBody());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to send push notification to user {} via OneSignal", userId, e);
            throw new RuntimeException("Failed to send push notification via OneSignal", e);
        }
    }
}
