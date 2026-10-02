package com.claircore.device.unit;

import com.claircore.device.domain.model.aggregates.Device;
import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.valueobjects.ApiKey;
import com.claircore.device.domain.model.valueobjects.ClaimToken;
import com.claircore.device.domain.model.valueobjects.DeviceStatus;
import com.claircore.device.domain.model.valueobjects.DeviceType;
import com.claircore.device.domain.model.valueobjects.HardwareId;
import com.claircore.device.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Device – pruebas unitarias de los agregados Device y DeviceAssignment")
class DeviceUnitTest {

    private static Device newDevice() {
        return new Device("SN-0001", "Sensor 0001", new HardwareId("CLAIR-0KBG"),
                ApiKey.generate(), new DeviceType("air-quality-v1"));
    }

    @Test
    @DisplayName("Happy path: reclamar un dispositivo emparejado lo asigna al espacio y al usuario")
    void claimingAPairedDeviceAssignsSpaceAndOwner() {
        // Business / User Story Rational (WS-US-11): el usuario reclama un sensor emparejado para ubicarlo en uno de sus espacios.
        // Arrange
        DeviceAssignment assignment = new DeviceAssignment(UUID.randomUUID(), ClaimToken.generate());
        UUID spaceId = UUID.randomUUID();
        UserId owner = new UserId(UUID.randomUUID());

        // Act
        assignment.claimToSpace(spaceId, owner);

        // Assert
        assertThat(assignment.getOwnerUserId()).isEqualTo(owner);
        assertThat(assignment.getSpaceId()).isEqualTo(spaceId);
        assertThat(assignment.getActivatedAt()).isNotNull();
        assertThat(assignment.getClaimToken()).isNull();
    }

    @Test
    @DisplayName("Límite superior: un evento de presencia más de 5 minutos en el futuro es rechazado")
    void presenceEventBeyondClockSkewIsRejected() {
        // Business / User Story Rational (WS-US-14): el estado de conexión no debe aceptar relojes desfasados del borde.
        // Arrange
        DeviceAssignment assignment = new DeviceAssignment(UUID.randomUUID(), ClaimToken.generate());
        Instant tooFarAhead = Instant.now().plus(DeviceAssignment.MAX_PRESENCE_CLOCK_SKEW).plusSeconds(60);

        // Act + Assert
        assertThatThrownBy(() -> assignment.updatePresence(DeviceStatus.ONLINE, tooFarAhead))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Presence event is too far in the future");
    }

    @Test
    @DisplayName("Límite inferior: un evento de presencia igual o anterior al último aplicado se ignora")
    void presenceEventNotNewerThanTheLastOneIsIgnored() {
        // Business / User Story Rational (WS-US-14): eventos duplicados o desordenados no deben alterar el estado mostrado.
        // Arrange
        DeviceAssignment assignment = new DeviceAssignment(UUID.randomUUID(), ClaimToken.generate());
        Instant firstEvent = Instant.now().minusSeconds(30);
        assignment.updatePresence(DeviceStatus.ONLINE, firstEvent);

        // Act
        boolean changed = assignment.updatePresence(DeviceStatus.OFFLINE, firstEvent);

        // Assert
        assertThat(changed).isFalse();
        assertThat(assignment.getStatus()).isEqualTo(DeviceStatus.ONLINE);
    }

    @Test
    @DisplayName("Datos insuficientes: un identificador de hardware con formato inválido es rechazado")
    void hardwareIdWithUnsupportedFormatIsRejected() {
        // Business / User Story Rational (WS-US-10): solo se emparejan unidades con un Hardware ID de fábrica válido.
        // Arrange
        String invalidHardwareId = "SENSOR-1";

        // Act + Assert
        assertThatThrownBy(() -> new HardwareId(invalidHardwareId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Hardware ID must match CLAIR-0KBG or HW-0001");
    }

    @Test
    @DisplayName("Estado inválido: un dispositivo ya reclamado no puede reclamarse otra vez")
    void anAlreadyClaimedDeviceCannotBeClaimedAgain() {
        // Business / User Story Rational (WS-US-11): un sensor solo puede tener un dueño a la vez.
        // Arrange
        DeviceAssignment assignment = new DeviceAssignment(UUID.randomUUID(), ClaimToken.generate());
        assignment.claimToSpace(UUID.randomUUID(), new UserId(UUID.randomUUID()));

        // Act + Assert
        assertThatThrownBy(() -> assignment.claimToSpace(UUID.randomUUID(), new UserId(UUID.randomUUID())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Device already claimed");
    }

    @Test
    @DisplayName("Estado inválido: renombrar un dispositivo con un nombre en blanco es rechazado")
    void renamingWithABlankNameIsRejected() {
        // Business / User Story Rational (WS-US-15): un dispositivo siempre debe ser identificable por su nombre.
        // Arrange
        Device device = newDevice();

        // Act + Assert
        assertThatThrownBy(() -> device.updateName("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Device name must not be null or blank");
        assertThat(device.getName()).isEqualTo("Sensor 0001");
    }

    @Test
    @DisplayName("Condicional A: un evento OFFLINE siempre cambia el estado aunque el dispositivo esté en STANDBY")
    void offlineAlwaysAppliesEvenFromStandby() {
        // Business / User Story Rational (WS-US-14): perder conexión siempre debe reflejarse en el estado del dispositivo.
        // Arrange
        DeviceAssignment assignment = new DeviceAssignment(UUID.randomUUID(), ClaimToken.generate());
        assignment.markStandby();

        // Act
        boolean changed = assignment.updatePresence(DeviceStatus.OFFLINE, Instant.now());

        // Assert
        assertThat(changed).isTrue();
        assertThat(assignment.getStatus()).isEqualTo(DeviceStatus.OFFLINE);
    }

    @Test
    @DisplayName("Condicional B: un evento ONLINE no saca al dispositivo de STANDBY, solo refresca lastSeenAt")
    void onlineDoesNotLeaveStandby() {
        // Business / User Story Rational (WS-US-14): STANDBY es un modo elegido por el usuario y solo lo cambia un comando.
        // Arrange
        DeviceAssignment assignment = new DeviceAssignment(UUID.randomUUID(), ClaimToken.generate());
        assignment.markStandby();
        Instant occurredAt = Instant.now();

        // Act
        boolean changed = assignment.updatePresence(DeviceStatus.ONLINE, occurredAt);

        // Assert
        assertThat(changed).isTrue();
        assertThat(assignment.getStatus()).isEqualTo(DeviceStatus.STANDBY);
        assertThat(assignment.getLastSeenAt()).isEqualTo(occurredAt);
    }

    @Test
    @DisplayName("Integridad: restablecer el nombre devuelve el nombre de fábrica")
    void resettingTheNameRestoresTheFactoryName() {
        // Business / User Story Rational (WS-US-16): al resetear la asignación, el dispositivo vuelve a su identidad de fábrica.
        // Arrange
        Device device = newDevice();
        device.updateName("Sala principal");

        // Act
        device.resetNameToFactoryDefault();

        // Assert
        assertThat(device.getName()).isEqualTo("Sensor 0001");
        assertThat(device.getFactoryName()).isEqualTo("Sensor 0001");
    }

    @Test
    @DisplayName("Integridad: marcar un dispositivo en línea registra su estado y su lastSeenAt")
    void markingOnlineRecordsStatusAndLastSeen() {
        // Business / User Story Rational (WS-US-14): la consulta de estado devuelve el deviceId, el status y el lastSeenAt.
        // Arrange
        UUID deviceId = UUID.randomUUID();
        DeviceAssignment assignment = new DeviceAssignment(deviceId, ClaimToken.generate());
        Instant before = Instant.now();

        // Act
        assignment.markOnline();

        // Assert
        assertThat(assignment.getDeviceId()).isEqualTo(deviceId);
        assertThat(assignment.getStatus()).isEqualTo(DeviceStatus.ONLINE);
        assertThat(assignment.getLastSeenAt()).isNotNull().isAfterOrEqualTo(before);
    }
}
