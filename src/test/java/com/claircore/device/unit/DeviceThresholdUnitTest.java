package com.claircore.device.unit;

import com.claircore.device.application.internal.commandservices.DeviceThresholdCommandServiceImpl;
import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.commands.WriteDeviceThresholdCommand;
import com.claircore.device.domain.model.valueobjects.ClaimToken;
import com.claircore.device.domain.model.valueobjects.DeviceMetricThresholdConfiguration;
import com.claircore.device.domain.model.valueobjects.DeviceThresholdWriteIntent;
import com.claircore.device.domain.model.valueobjects.MetricThreshold;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.domain.repositories.DeviceAssignmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Device Thresholds – pruebas unitarias de la configuración de umbrales")
class DeviceThresholdUnitTest {

    private static final UUID DEVICE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UserId OWNER = new UserId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));

    @Test
    @DisplayName("Happy path: un umbral de PM2.5 válido queda configurado y habilitado")
    void aValidPm25ThresholdIsConfigured() {
        // Business / User Story Rational (WS-US-18): el usuario define cuándo la calidad del aire debe generar alertas.
        // Arrange
        BigDecimal value = new BigDecimal("35.50");

        // Act
        var threshold = new DeviceMetricThresholdConfiguration(MetricThreshold.PM25, value, true);

        // Assert
        assertThat(threshold.metric()).isEqualTo(MetricThreshold.PM25);
        assertThat(threshold.value()).isEqualByComparingTo("35.50");
        assertThat(threshold.enabled()).isTrue();
    }

    @Test
    @DisplayName("Límite superior: un umbral de CO2 muy alto (5000 ppm) es aceptado")
    void aVeryHighCo2ThresholdIsAccepted() {
        // Business / User Story Rational (WS-US-18): ambientes industriales pueden necesitar umbrales de CO2 altos.
        // Arrange
        BigDecimal highValue = new BigDecimal("5000");

        // Act
        var threshold = new DeviceMetricThresholdConfiguration(MetricThreshold.CO2, highValue, true);

        // Assert
        assertThat(threshold.value()).isEqualByComparingTo("5000");
    }

    @Test
    @DisplayName("Límite inferior: un umbral exactamente en cero es aceptado")
    void aZeroThresholdIsAccepted() {
        // Business / User Story Rational (WS-US-18): cero es el valor mínimo válido para cualquier métrica.
        // Arrange
        BigDecimal zero = BigDecimal.ZERO;

        // Act
        var threshold = new DeviceMetricThresholdConfiguration(MetricThreshold.HUMIDITY, zero, true);

        // Assert
        assertThat(threshold.value()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Límite inferior: un umbral negativo es rechazado")
    void aNegativeThresholdIsRejected() {
        // Business / User Story Rational (WS-US-18): ninguna métrica de calidad del aire admite límites negativos.
        // Arrange
        BigDecimal negative = new BigDecimal("-0.01");

        // Act + Assert
        assertThatThrownBy(() -> new DeviceMetricThresholdConfiguration(MetricThreshold.PM25, negative, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Value must not be negative");
    }

    @Test
    @DisplayName("Datos insuficientes: un umbral sin métrica es rechazado")
    void aThresholdWithoutMetricIsRejected() {
        // Business / User Story Rational (WS-US-18): un umbral sin métrica no se puede evaluar.
        // Arrange
        BigDecimal value = new BigDecimal("50");

        // Act + Assert
        assertThatThrownBy(() -> new DeviceMetricThresholdConfiguration(null, value, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Metric must not be null");
    }

    @Test
    @DisplayName("Estado inválido: registrar un umbral para una métrica que ya tiene uno es rechazado")
    void registeringADuplicateThresholdIsRejected() {
        // Business / User Story Rational (WS-US-18): una métrica que ya posee un umbral registrado no admite otro.
        // Arrange
        DeviceAssignment assignment = new DeviceAssignment(DEVICE_ID, ClaimToken.generate());
        assignment.claimToSpace(UUID.randomUUID(), OWNER);
        assignment.putConfigurationValue("threshold.PM25", "{\"metric\":\"PM25\",\"value\":35,\"enabled\":true}");
        DeviceAssignmentRepository repository = mock(DeviceAssignmentRepository.class);
        when(repository.findByDeviceIdForUpdate(DEVICE_ID)).thenReturn(Optional.of(assignment));
        var service = new DeviceThresholdCommandServiceImpl(repository, new ObjectMapper());
        var duplicate = new WriteDeviceThresholdCommand(DEVICE_ID, OWNER, MetricThreshold.PM25,
                new BigDecimal("50"), true, DeviceThresholdWriteIntent.CREATE);

        // Act + Assert
        assertThatThrownBy(() -> service.handle(duplicate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Threshold already exists for the specified metric");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Condicional A: un umbral deshabilitado conserva su valor configurado")
    void aDisabledThresholdKeepsItsValue() {
        // Business / User Story Rational (WS-US-19): desactivar el monitoreo no debe perder el límite elegido.
        // Arrange
        BigDecimal value = new BigDecimal("28.0");

        // Act
        var threshold = new DeviceMetricThresholdConfiguration(MetricThreshold.TEMPERATURE, value, false);

        // Assert
        assertThat(threshold.enabled()).isFalse();
        assertThat(threshold.value()).isEqualByComparingTo("28.0");
    }

    @Test
    @DisplayName("Condicional B: la intención UPDATE se conserva en el comando de escritura")
    void updateIntentIsPreservedInTheCommand() {
        // Business / User Story Rational (WS-US-19): actualizar un umbral existente es distinto de crear uno nuevo.
        // Arrange
        BigDecimal value = new BigDecimal("60");

        // Act
        var command = new WriteDeviceThresholdCommand(DEVICE_ID, OWNER, MetricThreshold.HUMIDITY, value, true,
                DeviceThresholdWriteIntent.UPDATE);

        // Assert
        assertThat(command.intent()).isEqualTo(DeviceThresholdWriteIntent.UPDATE);
        assertThat(command.metric()).isEqualTo(MetricThreshold.HUMIDITY);
    }

    @Test
    @DisplayName("Integridad: agregar y quitar la configuración de un umbral deja la asignación sin él")
    void addingAndRemovingAThresholdLeavesNoTrace() {
        // Business / User Story Rational (WS-US-20): eliminar un umbral desactiva por completo el monitoreo de esa métrica.
        // Arrange
        DeviceAssignment assignment = new DeviceAssignment(DEVICE_ID, ClaimToken.generate());
        assignment.putConfigurationValue("threshold.PM25", "{\"metric\":\"PM25\",\"value\":35,\"enabled\":true}");

        // Act
        assignment.removeConfigurationValue("threshold.PM25");

        // Assert
        assertThat(assignment.findConfigurationValue("threshold.PM25")).isEmpty();
        assertThat(assignment.getConfiguration()).isEmpty();
    }

    @Test
    @DisplayName("Integridad: la etiqueta y la unidad de cada métrica son las que ve el usuario")
    void metricsExposeTheirLabelAndUnit() {
        // Business / User Story Rational (WS-US-17): las apps muestran cada umbral con su unidad correcta.
        // Arrange
        MetricThreshold pm25 = MetricThreshold.PM25;

        // Act
        String label = pm25.label();
        String unit = pm25.unit();

        // Assert
        assertThat(label).isEqualTo("PM2.5");
        assertThat(unit).isEqualTo("µg/m³");
        assertThat(MetricThreshold.CO2.unit()).isEqualTo("ppm");
    }
}
