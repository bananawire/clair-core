package com.claircore.assistant.application.internal.context;

import com.claircore.alerting.interfaces.acl.AlertDetails;
import com.claircore.assistant.application.internal.outboundservices.acl.ExternalAlertingService;
import com.claircore.assistant.application.internal.outboundservices.acl.ExternalDeviceService;
import com.claircore.assistant.application.internal.outboundservices.acl.ExternalEvaluationService;
import com.claircore.assistant.domain.model.valueobjects.AssistantLocale;
import com.claircore.device.interfaces.acl.ThresholdSummary;
import com.claircore.evaluation.interfaces.acl.TelemetryReading;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builds the system message: the assistant's rules plus a CONTEXT block with the user's own
 * devices, latest readings, thresholds and open alerts.
 *
 * <p>Privacy: only names, measurements and alert facts reach the model. The user's email and every
 * identifier (user, device, space, alert) stay on the server. Readings recorded before a device's
 * {@code visibleSince} belong to its previous owner and are never included.
 */
@Component
public class AssistantContextAssembler {

    static final int MAX_ACTIVE_ALERTS = 10;
    private static final int MAX_NAME_LENGTH = 60;

    private static final String RULES = "Eres el asistente de emergencia de Clair Niño. Responde breve, en lenguaje simple, "
            + "basándote solo en estas guías oficiales: {resumen}. Cita la fuente (MINSA o INDECI). "
            + "Si no está en las guías, dilo y recomienda seguir las alertas oficiales.";

    private final ExternalDeviceService externalDeviceService;
    private final ExternalEvaluationService externalEvaluationService;
    private final ExternalAlertingService externalAlertingService;
    private final int maxContextDevices;
    private final String systemRules;

    public AssistantContextAssembler(
            ExternalDeviceService externalDeviceService,
            ExternalEvaluationService externalEvaluationService,
            ExternalAlertingService externalAlertingService,
            @Value("${assistant.max-context-devices:10}") int maxContextDevices,
            @Value("classpath:assistant/emergency-guide.txt") Resource emergencyGuide) throws IOException {
        this.externalDeviceService = externalDeviceService;
        this.externalEvaluationService = externalEvaluationService;
        this.externalAlertingService = externalAlertingService;
        this.maxContextDevices = maxContextDevices;
        String summary = emergencyGuide.getContentAsString(StandardCharsets.UTF_8).strip();
        if (summary.isBlank()) {
            throw new IllegalArgumentException("The emergency guide must not be empty");
        }
        this.systemRules = RULES.replace("{resumen}", summary);
    }

    public String buildSystemPrompt(UUID userId, AssistantLocale locale, Instant now) {
        var prompt = new StringBuilder(systemRules);
        prompt.append("\nCONTEXT (generated ").append(now.truncatedTo(ChronoUnit.MINUTES)).append(")\n");
        appendDevices(prompt, userId);
        appendAlerts(prompt, userId);
        return prompt.toString();
    }

    private void appendDevices(StringBuilder prompt, UUID userId) {
        List<UUID> allDeviceIds = externalDeviceService.findDeviceIdsByOwnerId(userId);
        if (allDeviceIds.isEmpty()) {
            prompt.append("Devices: none registered yet.\n");
            return;
        }
        List<UUID> deviceIds = allDeviceIds.stream().limit(maxContextDevices).toList();
        Map<UUID, String> deviceNames = externalDeviceService.findDeviceNamesByDeviceIds(deviceIds);
        Map<UUID, UUID> spaceByDevice = deviceIds.stream()
                .flatMap(id -> externalDeviceService.findSpaceIdByDeviceId(id).stream().map(space -> Map.entry(id, space)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<UUID, String> spaceNames = spaceByDevice.isEmpty()
                ? Map.of()
                : externalDeviceService.findSpaceNamesBySpaceIds(spaceByDevice.values().stream().distinct().toList());

        prompt.append("Devices (").append(allDeviceIds.size()).append(" registered");
        if (allDeviceIds.size() > deviceIds.size()) {
            prompt.append(", first ").append(deviceIds.size()).append(" shown");
        }
        prompt.append("):\n");
        for (UUID deviceId : deviceIds) {
            prompt.append("- \"").append(safeName(deviceNames.get(deviceId), "Unnamed device")).append('"');
            UUID spaceId = spaceByDevice.get(deviceId);
            if (spaceId != null) {
                prompt.append(" in space \"").append(safeName(spaceNames.get(spaceId), "Unnamed space")).append('"');
            }
            prompt.append(": ").append(describeLatestReading(deviceId));
            List<ThresholdSummary> thresholds = externalDeviceService.findEnabledThresholdsByDeviceId(deviceId);
            if (!thresholds.isEmpty()) {
                prompt.append(" Thresholds: ").append(thresholds.stream()
                        .map(t -> t.metric() + " " + t.value().stripTrailingZeros().toPlainString())
                        .collect(Collectors.joining(", "))).append('.');
            }
            prompt.append('\n');
        }
    }

    private String describeLatestReading(UUID deviceId) {
        Instant visibleSince = externalDeviceService.findVisibleSinceByDeviceId(deviceId).orElse(null);
        Optional<TelemetryReading> latest = externalEvaluationService.findLatestReading(deviceId, visibleSince);
        return latest.map(r -> String.format(Locale.ROOT,
                        "CO2 %.0f ppm, PM2.5 %.1f ug/m3, temperature %.1f C, humidity %.0f%%, measured %s.",
                        r.co2(), r.pm2_5(), r.temperature(), r.humidity(), r.recordedAt().truncatedTo(ChronoUnit.MINUTES)))
                .orElse("no readings yet.");
    }

    private void appendAlerts(StringBuilder prompt, UUID userId) {
        List<AlertDetails> alerts = externalAlertingService.findActiveAlertsByOwnerId(userId, MAX_ACTIVE_ALERTS);
        if (alerts.isEmpty()) {
            prompt.append("Active alerts: none.\n");
            return;
        }
        prompt.append("Active alerts (").append(alerts.size()).append("):\n");
        var lines = new ArrayList<String>();
        for (AlertDetails alert : alerts) {
            lines.add(String.format(Locale.ROOT, "- \"%s\": %s %s (threshold %s), severity %s, since %s.",
                    safeName(alert.deviceName(), "Unnamed device"),
                    alert.metric(),
                    alert.actualValue() == null ? "?" : alert.actualValue().stripTrailingZeros().toPlainString(),
                    alert.thresholdValue() == null ? "?" : alert.thresholdValue().stripTrailingZeros().toPlainString(),
                    alert.severity(),
                    alert.occurredAt() == null ? "unknown" : alert.occurredAt().truncatedTo(ChronoUnit.MINUTES)));
        }
        prompt.append(String.join("\n", lines)).append('\n');
    }

    /** Names are typed by users: keep them on one line, unquoted and short so they cannot pose as rules. */
    static String safeName(String name, String fallback) {
        if (name == null || name.isBlank()) return fallback;
        String cleaned = name.replaceAll("[\\p{Cntrl}\"`]", " ").replaceAll("\\s+", " ").strip();
        if (cleaned.isEmpty()) return fallback;
        return cleaned.length() <= MAX_NAME_LENGTH ? cleaned : cleaned.substring(0, MAX_NAME_LENGTH);
    }
}
