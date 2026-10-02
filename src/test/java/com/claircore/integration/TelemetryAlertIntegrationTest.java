package com.claircore.integration;

import com.claircore.alerting.application.queryservices.AlertQueryService;
import com.claircore.alerting.domain.model.aggregates.Alert;
import com.claircore.alerting.domain.model.valueobjects.AlertSeverity;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.domain.model.valueobjects.MetricType;
import com.claircore.device.application.commandservices.DeviceThresholdCommandService;
import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.aggregates.Space;
import com.claircore.device.domain.model.commands.WriteDeviceThresholdCommand;
import com.claircore.device.domain.model.valueobjects.DeviceThresholdWriteIntent;
import com.claircore.device.domain.model.valueobjects.MetricThreshold;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.evaluation.interfaces.events.TelemetryRecordedIntegrationEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The reading enters through the integration event Evaluation publishes after storing it. Storing
 * itself is a PostgreSQL-only upsert ({@code ON CONFLICT DO NOTHING}) that H2 cannot run, so it is
 * covered by the system test against PostgreSQL; here the event is committed in a real transaction
 * and everything downstream (Device thresholds through the ACL, Alerting, persistence) is real.
 */
@DisplayName("Integración Device → Evaluation → Alerting: la telemetría que supera un umbral abre una alerta")
class TelemetryAlertIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DeviceThresholdCommandService deviceThresholdCommandService;
    @Autowired
    private AlertQueryService alertQueryService;
    @Autowired
    private ApplicationEventPublisher events;
    @Autowired
    private PlatformTransactionManager transactions;

    private DeviceAssignment ownedDeviceWithPm25Threshold(String thresholdValue) {
        UserId owner = newUser();
        Space space = createSpace(owner, createOrganization(owner, "Casa"), "Dormitorio");
        DeviceAssignment device = pairAndClaim(owner, space, "Sensor dormitorio");
        deviceThresholdCommandService.handle(new WriteDeviceThresholdCommand(device.getDeviceId(), owner,
                MetricThreshold.PM25, new BigDecimal(thresholdValue), true, DeviceThresholdWriteIntent.CREATE));
        return device;
    }

    /** Publishes the reading the way Evaluation does: inside a transaction, delivered after commit. */
    private void recordTelemetry(UUID deviceId, double pm25, Instant recordedAt) {
        var event = new TelemetryRecordedIntegrationEvent(deviceId, 450.0, 22.0, 50.0, 5.0, pm25, 20.0,
                "ONLINE", "WiFi", -50, "Peru", 90, "STABLE", 3600L, UUID.randomUUID().toString(), recordedAt);
        new TransactionTemplate(transactions).executeWithoutResult(status -> events.publishEvent(event));
    }

    @Test
    @DisplayName("Una lectura de PM2.5 por encima del umbral abre una alerta CRITICAL para el dispositivo")
    void aReadingAboveTheThresholdOpensAnAlert() {
        // Business / User Story Rational (WS-US-18, WS-US-49, WS-US-34): el usuario configura un umbral, el borde envía
        // una lectura que lo supera y el usuario ve la alerta en su panel.
        // Arrange
        DeviceAssignment device = ownedDeviceWithPm25Threshold("35");

        // Act
        recordTelemetry(device.getDeviceId(), 80.0, Instant.now().minusSeconds(5));

        // Assert
        List<Alert> active = alertQueryService.findActiveByDeviceId(device.getDeviceId());
        assertThat(active).hasSize(1);
        Alert alert = active.getFirst();
        assertThat(alert.getMetric()).isEqualTo(MetricType.PM25);
        assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.ACTIVE);
        assertThat(alert.getSpaceName()).isEqualTo("Dormitorio");
    }

    @Test
    @DisplayName("Una lectura por debajo del umbral no abre ninguna alerta")
    void aReadingBelowTheThresholdOpensNoAlert() {
        // Business / User Story Rational (WS-US-49, WS-US-34): con aire limpio el usuario no recibe alertas falsas.
        // Arrange
        DeviceAssignment device = ownedDeviceWithPm25Threshold("35");

        // Act
        recordTelemetry(device.getDeviceId(), 12.0, Instant.now().minusSeconds(5));

        // Assert
        assertThat(alertQueryService.findActiveByDeviceId(device.getDeviceId())).isEmpty();
    }

    @Test
    @DisplayName("Una lectura posterior por debajo del umbral resuelve la alerta abierta")
    void aLaterCleanReadingResolvesTheAlert() {
        // Business / User Story Rational (WS-US-36, WS-US-49): cuando el aire vuelve a la normalidad la alerta se cierra sola.
        // Arrange
        DeviceAssignment device = ownedDeviceWithPm25Threshold("35");
        recordTelemetry(device.getDeviceId(), 80.0, Instant.now().minusSeconds(10));

        // Act
        recordTelemetry(device.getDeviceId(), 10.0, Instant.now().minusSeconds(5));

        // Assert
        assertThat(alertQueryService.findActiveByDeviceId(device.getDeviceId())).isEmpty();
    }
}
