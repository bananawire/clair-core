package com.claircore.alerting.unit;

import com.claircore.alerting.application.internal.commandservices.AlertCommandServiceImpl;
import com.claircore.alerting.application.internal.outboundservices.acl.ExternalAlertingDeviceService;
import com.claircore.alerting.application.internal.outboundservices.acl.ExternalAlertingThresholdService;
import com.claircore.alerting.domain.model.aggregates.Alert;
import com.claircore.alerting.domain.model.commands.EvaluateTelemetryForAlertsCommand;
import com.claircore.alerting.domain.model.valueobjects.AlertSeverity;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.domain.model.valueobjects.MetricType;
import com.claircore.alerting.domain.repositories.AlertRepository;
import com.claircore.device.interfaces.acl.ThresholdSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Alert – pruebas unitarias del agregado y de la regla de severidad")
class AlertUnitTest {

    private static final UUID DEVICE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant OCCURRED_AT = Instant.parse("2026-10-01T15:00:00Z");

    private static Alert newPm25Alert() {
        return new Alert(DEVICE_ID, UUID.randomUUID(), "Sala", "Sensor 1", MetricType.PM25,
                new BigDecimal("50"), new BigDecimal("80"), "PM2.5 threshold exceeded",
                AlertSeverity.CRITICAL, OCCURRED_AT);
    }

    /** Runs the real alert service with mocked collaborators and returns the alert it opens. */
    private static Alert openAlertFor(String thresholdValue, String pm25Reading) {
        AlertRepository repository = mock(AlertRepository.class);
        ExternalAlertingThresholdService thresholds = mock(ExternalAlertingThresholdService.class);
        ExternalAlertingDeviceService devices = mock(ExternalAlertingDeviceService.class);
        when(thresholds.fetchEnabledThresholdsByDeviceId(DEVICE_ID))
                .thenReturn(List.of(new ThresholdSummary("PM25", new BigDecimal(thresholdValue), true)));
        when(repository.findFirstByDeviceIdAndMetricAndStatusIn(eq(DEVICE_ID), eq(MetricType.PM25), anyCollection()))
                .thenReturn(Optional.empty());
        when(repository.nextTransitionSequence()).thenReturn(1L);
        when(repository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new AlertCommandServiceImpl(repository, thresholds, devices, mock(ApplicationEventPublisher.class));

        service.handle(new EvaluateTelemetryForAlertsCommand(DEVICE_ID, OCCURRED_AT,
                new BigDecimal(pm25Reading), new BigDecimal("400"), new BigDecimal("22"), new BigDecimal("50")));

        ArgumentCaptor<Alert> saved = ArgumentCaptor.forClass(Alert.class);
        verify(repository).save(saved.capture());
        return saved.getValue();
    }

    @Test
    @DisplayName("Happy path: una alerta nueva nace ACTIVA con los valores de la lectura")
    void aNewAlertStartsActive() {
        // Business / User Story Rational (WS-US-34): el usuario ve en su panel las alertas abiertas.
        // Arrange + Act
        Alert alert = newPm25Alert();

        // Assert
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.ACTIVE);
        assertThat(alert.getActualValue()).isEqualByComparingTo("80");
        assertThat(alert.getThresholdValue()).isEqualByComparingTo("50");
        assertThat(alert.getResolvedAt()).isNull();
    }

    @Test
    @DisplayName("Límite superior: una lectura de 1.5 veces el umbral es CRITICAL")
    void readingAtOneAndAHalfTimesTheThresholdIsCritical() {
        // Business / User Story Rational (WS-US-49): superar el umbral en 50% o más es un riesgo crítico para la salud.
        // Arrange + Act
        Alert alert = openAlertFor("50", "75");

        // Assert
        assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    @Test
    @DisplayName("Límite inferior: una lectura exactamente igual al umbral abre una alerta LOW")
    void readingEqualToTheThresholdOpensALowAlert() {
        // Business / User Story Rational (WS-US-49): alcanzar el umbral ya cuenta como incumplimiento.
        // Arrange + Act
        Alert alert = openAlertFor("50", "50");

        // Assert
        assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.LOW);
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.ACTIVE);
    }

    @Test
    @DisplayName("Datos insuficientes: una lectura sin todas sus métricas no puede evaluarse")
    void aReadingWithMissingMetricsCannotBeEvaluated() {
        // Business / User Story Rational (WS-US-49): la telemetría se registra con co2, temperature, humidity y
        // material particulado; sin esos campos no hay evaluación posible.
        // Arrange
        BigDecimal missingPm25 = null;

        // Act + Assert
        assertThatThrownBy(() -> new EvaluateTelemetryForAlertsCommand(DEVICE_ID, OCCURRED_AT, missingPm25,
                new BigDecimal("400"), new BigDecimal("22"), new BigDecimal("50")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Telemetry values must not be null");
    }

    @Test
    @DisplayName("Estado inválido: una métrica con una alerta ya abierta no abre una segunda alerta")
    void anOpenAlertIsNotDuplicated() {
        // Business / User Story Rational (WS-US-36): los registros de alertas del dispositivo muestran un incidente
        // por métrica, no una alerta repetida por cada lectura.
        // Arrange
        AlertRepository repository = mock(AlertRepository.class);
        ExternalAlertingThresholdService thresholds = mock(ExternalAlertingThresholdService.class);
        when(thresholds.fetchEnabledThresholdsByDeviceId(DEVICE_ID))
                .thenReturn(List.of(new ThresholdSummary("PM25", new BigDecimal("50"), true)));
        when(repository.findFirstByDeviceIdAndMetricAndStatusIn(eq(DEVICE_ID), eq(MetricType.PM25), anyCollection()))
                .thenReturn(Optional.of(newPm25Alert()));
        var service = new AlertCommandServiceImpl(repository, thresholds, mock(ExternalAlertingDeviceService.class),
                mock(ApplicationEventPublisher.class));

        // Act
        service.handle(new EvaluateTelemetryForAlertsCommand(DEVICE_ID, OCCURRED_AT,
                new BigDecimal("90"), new BigDecimal("400"), new BigDecimal("22"), new BigDecimal("50")));

        // Assert
        verify(repository, never()).save(any(Alert.class));
    }

    @Test
    @DisplayName("Condicional A: una lectura entre 1.2 y 1.5 veces el umbral es WARNING")
    void readingBetweenOnePointTwoAndOnePointFiveIsWarning() {
        // Business / User Story Rational (WS-US-49): un exceso moderado debe advertir sin alarmar como crítico.
        // Arrange + Act
        Alert alert = openAlertFor("50", "60");

        // Assert
        assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.WARNING);
    }

    @Test
    @DisplayName("Condicional B: una lectura justo debajo de 1.2 veces el umbral sigue siendo LOW")
    void readingJustBelowOnePointTwoIsLow() {
        // Business / User Story Rational (WS-US-49): excesos leves no deben escalar a advertencia.
        // Arrange + Act
        Alert alert = openAlertFor("50", "59.99");

        // Assert
        assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.LOW);
    }

    @Test
    @DisplayName("Integridad: resolver una alerta registra su cierre y conserva los valores originales")
    void resolvingKeepsOriginalValues() {
        // Business / User Story Rational (WS-US-36): el diagnóstico por dispositivo necesita la lectura que abrió la alerta.
        // Arrange
        Alert alert = newPm25Alert();
        Instant resolvedAt = OCCURRED_AT.plusSeconds(600);

        // Act
        alert.resolve(resolvedAt);

        // Assert
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(alert.getResolvedAt()).isEqualTo(resolvedAt);
        assertThat(alert.getActualValue()).isEqualByComparingTo("80");
        assertThat(alert.getOccurredAt()).isEqualTo(OCCURRED_AT);
    }

    @Test
    @DisplayName("Integridad: el mensaje de la alerta muestra la métrica, el valor leído y el umbral con su unidad")
    void alertMessageShowsMetricValueAndThreshold() {
        // Business / User Story Rational (WS-US-34): las alertas se visualizan en el panel de notificaciones con un
        // mensaje que el usuario entiende.
        // Arrange + Act
        Alert alert = openAlertFor("50", "80");

        // Assert
        assertThat(alert.getMessage()).isEqualTo("PM2.5 threshold exceeded: 80 µg/m³ (threshold: 50 µg/m³)");
    }
}
