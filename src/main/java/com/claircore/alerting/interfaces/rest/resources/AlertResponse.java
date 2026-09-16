package com.claircore.alerting.interfaces.rest.resources;

import com.claircore.alerting.domain.model.aggregates.Alert;
import com.claircore.alerting.domain.model.valueobjects.AlertSeverity;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.domain.model.valueobjects.MetricType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Alert response resource")
public record AlertResponse(
        @Schema(description = "Alert ID")
        UUID id,

        @Schema(description = "Device ID")
        UUID deviceId,

        @Schema(description = "Space ID")
        UUID spaceId,

        @Schema(description = "Metric type", example = "PM25")
        MetricType metric,

        @Schema(description = "Metric label", example = "PM2.5")
        String metricLabel,

        @Schema(description = "Metric unit", example = "µg/m³")
        String metricUnit,

        @Schema(description = "Threshold value that triggers the alert", example = "90.00")
        BigDecimal thresholdValue,

        @Schema(description = "Actual measured value", example = "95.50")
        BigDecimal actualValue,

        @Schema(description = "Alert message", example = "We detected a high PM2.5 level for this device.")
        String message,

        @Schema(description = "Alert status", example = "ACTIVE")
        AlertStatus status,

        @Schema(description = "Alert severity", example = "CRITICAL")
        AlertSeverity severity,

        @Schema(description = "Space name", example = "Floor 2")
        String spaceName,

        @Schema(description = "Device name", example = "Living Room Sensor")
        String deviceName,

        @Schema(description = "When the alert occurred")
        Instant occurredAt,

        @Schema(description = "When the alert was resolved", nullable = true)
        Instant resolvedAt,

        @Schema(description = "When the alert was created")
        Instant createdAt
) {
    public static AlertResponse from(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getDeviceId(),
                alert.getSpaceId(),
                alert.getMetric(),
                alert.getMetric().label(),
                alert.getMetric().unit(),
                alert.getThresholdValue(),
                alert.getActualValue(),
                alert.getMessage(),
                alert.getStatus(),
                alert.getSeverity(),
                alert.getSpaceName(),
                alert.getDeviceName(),
                alert.getOccurredAt(),
                alert.getResolvedAt(),
                alert.getCreatedAt()
        );
    }

    public static AlertResponse from(Alert alert, String resolvedSpaceName, String resolvedDeviceName) {
        String spaceName = (resolvedSpaceName != null && !resolvedSpaceName.isBlank())
                ? resolvedSpaceName
                : alert.getSpaceName();
        String deviceName = (resolvedDeviceName != null && !resolvedDeviceName.isBlank())
                ? resolvedDeviceName
                : alert.getDeviceName();

        return new AlertResponse(
                alert.getId(),
                alert.getDeviceId(),
                alert.getSpaceId(),
                alert.getMetric(),
                alert.getMetric().label(),
                alert.getMetric().unit(),
                alert.getThresholdValue(),
                alert.getActualValue(),
                alert.getMessage(),
                alert.getStatus(),
                alert.getSeverity(),
                spaceName,
                deviceName,
                alert.getOccurredAt(),
                alert.getResolvedAt(),
                alert.getCreatedAt()
        );
    }
}
