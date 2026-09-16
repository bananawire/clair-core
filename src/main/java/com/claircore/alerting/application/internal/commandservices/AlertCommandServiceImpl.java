package com.claircore.alerting.application.internal.commandservices;

import com.claircore.alerting.application.commandservices.AlertCommandService;
import com.claircore.alerting.application.internal.outboundservices.acl.ExternalAlertingDeviceService;
import com.claircore.alerting.application.internal.outboundservices.acl.ExternalAlertingThresholdService;
import com.claircore.alerting.domain.model.aggregates.Alert;
import com.claircore.alerting.domain.model.commands.EvaluateTelemetryForAlertsCommand;
import com.claircore.alerting.domain.model.valueobjects.AlertSeverity;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.domain.model.valueobjects.MetricType;
import com.claircore.alerting.domain.repositories.AlertRepository;
import com.claircore.alerting.interfaces.events.AlertIncidentChangedIntegrationEvent;
import com.claircore.device.interfaces.acl.ThresholdSummary;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Write-side service for the alerting bounded context. With the external edge integration
 * retired, lifecycle events are now delivered to in-process consumers (e.g. notifications) by
 * publishing a Spring {@link AlertIncidentChangedIntegrationEvent}; the previous edge webhook
 * fan-out has been removed.
 */
@Service
public class AlertCommandServiceImpl implements AlertCommandService {

    private static final List<AlertStatus> OPEN_STATUSES = List.of(AlertStatus.ACTIVE, AlertStatus.ACKNOWLEDGED);

    private final AlertRepository alertRepository;
    private final ExternalAlertingThresholdService externalThresholdService;
    private final ExternalAlertingDeviceService externalDeviceService;
    private final ApplicationEventPublisher eventPublisher;

    public AlertCommandServiceImpl(
            AlertRepository alertRepository,
            ExternalAlertingThresholdService externalThresholdService,
            ExternalAlertingDeviceService externalDeviceService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.alertRepository = alertRepository;
        this.externalThresholdService = externalThresholdService;
        this.externalDeviceService = externalDeviceService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void handle(EvaluateTelemetryForAlertsCommand command) {
        var spaceId = externalDeviceService.fetchSpaceIdByDeviceId(command.deviceId()).orElse(null);
        List<ThresholdSummary> enabled = externalThresholdService.fetchEnabledThresholdsByDeviceId(command.deviceId());

        Map<MetricType, BigDecimal> telemetry = telemetryValues(command);
        for (ThresholdSummary threshold : enabled) {
            MetricType metric = MetricType.valueOf(threshold.metric());
            BigDecimal actual = telemetry.get(metric);
            if (actual == null) continue;

            int comparison = actual.compareTo(threshold.value());
            if (comparison >= 0) {
                alertRepository.findFirstByDeviceIdAndMetricAndStatusIn(command.deviceId(), metric, OPEN_STATUSES)
                        .ifPresentOrElse(
                                existing -> {
                                    // Already active; avoid spamming duplicate alerts for every reading.
                                },
                                () -> {
                                    var spaceName = externalDeviceService.fetchSpaceNameBySpaceId(spaceId).orElse(null);
                                    var deviceName = externalDeviceService.fetchDeviceNameByDeviceId(command.deviceId()).orElse(null);
                                    var severity = calculateSeverity(actual, threshold.value());
                                    Alert opened = new Alert(
                                            command.deviceId(),
                                            spaceId,
                                            spaceName,
                                            deviceName,
                                            metric,
                                            threshold.value(),
                                            actual,
                                            buildMessage(metric, threshold.value(), actual),
                                            severity,
                                            command.occurredAt()

                                            );

                                            opened.markTransition(alertRepository.nextTransitionSequence());

                                            Alert created = alertRepository.save(opened);

                                            publishIncidentChanged(created);
                                }
                        );
            } else {
                alertRepository.findFirstByDeviceIdAndMetricAndStatusIn(command.deviceId(), metric, OPEN_STATUSES)
                        .ifPresent(openAlert -> {
                            openAlert.resolve(command.occurredAt());
                            openAlert.markTransition(alertRepository.nextTransitionSequence());
                            Alert saved = alertRepository.save(openAlert);
                            publishIncidentChanged(saved);
                        });
            }
        }
    }

    /**
     * Publishes a lifecycle event over the in-process bus. Notifications and other alerting
     * listeners pick it up; the previous edge webhook fan-out is gone.
     */
    private void publishIncidentChanged(Alert alert) {
        String hardwareId = externalDeviceService.fetchHardwareIdByDeviceId(alert.getDeviceId())
                .orElse(alert.getDeviceId().toString());

        eventPublisher.publishEvent(new AlertIncidentChangedIntegrationEvent(
                alert.getId(),
                alert.getDeviceId(),
                hardwareId,
                alert.getSpaceId(),
                alert.getMetric().name(),
                alert.getThresholdValue(),
                alert.getActualValue(),
                alert.getMessage(),
                alert.getStatus().name(),
                alert.getOccurredAt(),
                alert.getResolvedAt()
        ));
    }

    private static Map<MetricType, BigDecimal> telemetryValues(EvaluateTelemetryForAlertsCommand command) {
        var map = new EnumMap<MetricType, BigDecimal>(MetricType.class);
        map.put(MetricType.PM25, command.pm25());
        map.put(MetricType.CO2, command.co2());
        map.put(MetricType.TEMPERATURE, command.temperature());
        map.put(MetricType.HUMIDITY, command.humidity());
        return map;
    }

    private static String buildMessage(MetricType metric, BigDecimal threshold, BigDecimal actual) {
        return "%s threshold exceeded: %s %s (threshold: %s %s)".formatted(
                metric.label(),
                actual.stripTrailingZeros().toPlainString(),
                metric.unit(),
                threshold.stripTrailingZeros().toPlainString(),
                metric.unit());
    }

    private static AlertSeverity calculateSeverity(BigDecimal actual, BigDecimal threshold) {
        if (threshold.compareTo(BigDecimal.ZERO) == 0) {
            return AlertSeverity.LOW;
        }
        BigDecimal ratio = actual.divide(threshold, 4, java.math.RoundingMode.HALF_UP);
        if (ratio.compareTo(new BigDecimal("1.5")) >= 0) {
            return AlertSeverity.CRITICAL;
        }
        if (ratio.compareTo(new BigDecimal("1.2")) >= 0) {
            return AlertSeverity.WARNING;
        }
        return AlertSeverity.LOW;
    }
}
