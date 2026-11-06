package com.claircore.integration;

import com.claircore.device.application.queryservices.DeviceQueryService;
import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.aggregates.Organization;
import com.claircore.device.domain.model.aggregates.Space;
import com.claircore.device.domain.model.queries.GetAssignedDeviceByIdForUserQuery;
import com.claircore.device.domain.model.queries.GetDevicesBySpaceForUserQuery;
import com.claircore.device.domain.model.queries.GetOrganizationByIdForUserQuery;
import com.claircore.device.domain.model.queries.GetSpaceByIdForUserQuery;
import com.claircore.device.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Integración multi-tenant: cada usuario solo ve sus organizaciones, espacios y dispositivos")
class MultiTenantIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DeviceQueryService deviceQueryService;

    @Test
    @DisplayName("El dueño ve su organización, su espacio y su dispositivo persistidos")
    void theOwnerSeesEverythingItCreated() {
        // Business / User Story Rational (WS-US-25, WS-US-30, WS-US-13): el dueño consulta sus propios recursos.
        // Arrange
        UserId owner = newUser();
        Organization organization = createOrganization(owner, "Oficina A");
        Space space = createSpace(owner, organization, "Recepción");
        DeviceAssignment device = pairAndClaim(owner, space, "Sensor recepción");

        // Act
        var foundOrganization = deviceQueryService.handle(new GetOrganizationByIdForUserQuery(organization.getId(), owner));
        var foundSpace = deviceQueryService.handle(new GetSpaceByIdForUserQuery(space.getId(), owner));
        var foundDevice = deviceQueryService.handle(new GetAssignedDeviceByIdForUserQuery(device.getDeviceId(), owner));

        // Assert
        assertThat(foundOrganization).get().extracting(Organization::getName).isEqualTo("Oficina A");
        assertThat(foundSpace).get().extracting(Space::getName).isEqualTo("Recepción");
        assertThat(foundDevice).isPresent();
        assertThat(foundDevice.get().device().getName()).isEqualTo("Sensor recepción");
    }

    @Test
    @DisplayName("Otro usuario no puede leer la organización, el espacio ni el dispositivo ajenos")
    void anotherUserCannotReadForeignResources() {
        // Business / User Story Rational (WS-US-25, WS-US-30, WS-US-13): los datos de un inquilino nunca se filtran a otro.
        // Arrange
        UserId owner = newUser();
        Organization organization = createOrganization(owner, "Oficina A");
        Space space = createSpace(owner, organization, "Recepción");
        DeviceAssignment device = pairAndClaim(owner, space, "Sensor recepción");
        UserId stranger = newUser();

        // Act
        var organizationSeenByStranger = deviceQueryService.handle(new GetOrganizationByIdForUserQuery(organization.getId(), stranger));
        var spaceSeenByStranger = deviceQueryService.handle(new GetSpaceByIdForUserQuery(space.getId(), stranger));
        var deviceSeenByStranger = deviceQueryService.handle(new GetAssignedDeviceByIdForUserQuery(device.getDeviceId(), stranger));

        // Assert
        assertThat(organizationSeenByStranger).isEmpty();
        assertThat(spaceSeenByStranger).isEmpty();
        assertThat(deviceSeenByStranger).isEmpty();
    }

    @Test
    @DisplayName("Listar los dispositivos de un espacio ajeno es denegado")
    void listingDevicesOfAForeignSpaceIsDenied() {
        // Business / User Story Rational (WS-US-12): solo el dueño del espacio puede listar sus sensores.
        // Arrange
        UserId owner = newUser();
        Space space = createSpace(owner, createOrganization(owner, "Oficina A"), "Recepción");
        pairAndClaim(owner, space, "Sensor recepción");
        UserId stranger = newUser();

        // Act + Assert
        assertThatThrownBy(() -> deviceQueryService.handle(new GetDevicesBySpaceForUserQuery(space.getId(), 0, 10, stranger)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Space does not belong to user");
    }
}
