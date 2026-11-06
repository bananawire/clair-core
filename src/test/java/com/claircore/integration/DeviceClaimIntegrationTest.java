package com.claircore.integration;

import com.claircore.device.application.queryservices.DeviceQueryService;
import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.aggregates.Organization;
import com.claircore.device.domain.model.aggregates.Space;
import com.claircore.device.domain.model.commands.ClaimDeviceCommand;
import com.claircore.device.domain.model.commands.PairDeviceCommand;
import com.claircore.device.domain.model.queries.GetDevicesBySpaceForUserQuery;
import com.claircore.device.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Integración Device: emparejar, reclamar y listar dispositivos de un espacio")
class DeviceClaimIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DeviceQueryService deviceQueryService;

    @Test
    @DisplayName("Un dispositivo emparejado y reclamado aparece en el listado de su espacio")
    void aClaimedDeviceIsListedInItsSpace() {
        // Business / User Story Rational (WS-US-10, WS-US-11, WS-US-12): el usuario empareja un sensor,
        // lo reclama con su código y luego lo ve en el espacio donde lo ubicó.
        // Arrange
        UserId owner = newUser();
        Organization organization = createOrganization(owner, "Casa");
        Space livingRoom = createSpace(owner, organization, "Sala");
        String hardwareId = registerFactoryDevice("Sensor sala");

        // Act
        DeviceAssignment paired = deviceCommandService.handle(new PairDeviceCommand(hardwareId));
        DeviceAssignment claimed = deviceCommandService.handle(
                new ClaimDeviceCommand(paired.getClaimToken().value(), livingRoom.getId(), owner));
        var devicesInSpace = deviceQueryService.handle(
                new GetDevicesBySpaceForUserQuery(livingRoom.getId(), 0, 10, owner));

        // Assert
        assertThat(claimed.getOwnerUserId()).isEqualTo(owner);
        assertThat(claimed.getSpaceId()).isEqualTo(livingRoom.getId());
        assertThat(devicesInSpace.items()).hasSize(1);
        assertThat(devicesInSpace.items().getFirst().device().getHardwareId().value()).isEqualTo(hardwareId);
        assertThat(devicesInSpace.items().getFirst().device().getName()).isEqualTo("Sensor sala");
    }

    @Test
    @DisplayName("El código de reclamo se consume: no se puede volver a usar para otro usuario")
    void theClaimTokenCannotBeReused() {
        // Business / User Story Rational (WS-US-11): el código de un solo uso evita que otra persona se apropie del sensor.
        // Arrange
        UserId owner = newUser();
        Space ownerSpace = createSpace(owner, createOrganization(owner, "Casa"), "Sala");
        UserId intruder = newUser();
        Space intruderSpace = createSpace(intruder, createOrganization(intruder, "Otra casa"), "Cuarto");
        DeviceAssignment paired = deviceCommandService.handle(new PairDeviceCommand(registerFactoryDevice("Sensor")));
        String token = paired.getClaimToken().value();
        deviceCommandService.handle(new ClaimDeviceCommand(token, ownerSpace.getId(), owner));

        // Act + Assert
        assertThatThrownBy(() -> deviceCommandService.handle(new ClaimDeviceCommand(token, intruderSpace.getId(), intruder)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid claim token");
    }

    @Test
    @DisplayName("Con plan Freemium, Billing limita a 1 el número de dispositivos reclamados")
    void freemiumPlanLimitsClaimedDevices() {
        // Business / User Story Rational (WS-US-11, WS-US-46): el plan del usuario, resuelto por Billing, limita cuántos
        // dispositivos puede reclamar.
        // Arrange
        UserId owner = newUser();
        Space space = createSpace(owner, createOrganization(owner, "Casa"), "Sala");
        pairAndClaim(owner, space, "Primer sensor");
        DeviceAssignment second = deviceCommandService.handle(new PairDeviceCommand(registerFactoryDevice("Segundo sensor")));

        // Act + Assert
        assertThatThrownBy(() -> deviceCommandService.handle(
                new ClaimDeviceCommand(second.getClaimToken().value(), space.getId(), owner)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("max allowed is 1");
    }
}
