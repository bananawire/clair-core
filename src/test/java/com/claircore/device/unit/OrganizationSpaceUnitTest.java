package com.claircore.device.unit;

import com.claircore.device.application.internal.commandservices.SpaceCommandServiceImpl;
import com.claircore.device.application.internal.outboundservices.acl.ExternalBillingService;
import com.claircore.device.domain.model.aggregates.Organization;
import com.claircore.device.domain.model.aggregates.Space;
import com.claircore.device.domain.model.commands.CreateOrganizationCommand;
import com.claircore.device.domain.model.commands.CreateSpaceCommand;
import com.claircore.device.domain.model.commands.DeleteSpaceCommand;
import com.claircore.device.domain.model.commands.UpdateSpaceNameCommand;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.domain.repositories.DeviceAssignmentRepository;
import com.claircore.device.domain.repositories.OrganizationRepository;
import com.claircore.device.domain.repositories.SpaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Organization y Space – pruebas unitarias de los agregados")
class OrganizationSpaceUnitTest {

    private static final UserId OWNER = new UserId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));

    @Test
    @DisplayName("Happy path: crear una organización la deja asociada a su dueño")
    void creatingAnOrganizationKeepsItsOwner() {
        // Business / User Story Rational (WS-US-24): una organización estructura los espacios de un usuario.
        // Arrange
        String name = "Oficina Miraflores";

        // Act
        Organization organization = new Organization(name, OWNER);

        // Assert
        assertThat(organization.getId()).isNotNull();
        assertThat(organization.getName()).isEqualTo(name);
        assertThat(organization.getOwnerUserId()).isEqualTo(OWNER);
    }

    @Test
    @DisplayName("Límite superior: con el plan que permite 1 espacio, crear el segundo es rechazado")
    void creatingASpaceBeyondThePlanLimitIsRejected() {
        // Business / User Story Rational (WS-US-46): el plan activo del usuario se usa para validar sus límites de uso
        // en otros módulos, aquí al crear un espacio (WS-US-29).
        // Arrange
        Organization organization = new Organization("Oficina", OWNER);
        SpaceRepository spaces = mock(SpaceRepository.class);
        OrganizationRepository organizations = mock(OrganizationRepository.class);
        ExternalBillingService billing = mock(ExternalBillingService.class);
        when(organizations.findById(organization.getId())).thenReturn(Optional.of(organization));
        when(spaces.countByOwnerUserId(OWNER)).thenReturn(1);
        when(billing.getMaxSpaces(OWNER.userId())).thenReturn(1);
        var service = new SpaceCommandServiceImpl(spaces, organizations, mock(DeviceAssignmentRepository.class), billing);

        // Act + Assert
        assertThatThrownBy(() -> service.handle(new CreateSpaceCommand("Segundo espacio", organization.getId(), OWNER)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot create space. User has 1 spaces, max allowed is 1");
        verify(spaces, never()).save(any());
    }

    @Test
    @DisplayName("Estado inválido: eliminar un espacio con sensores registrados es rechazado")
    void deletingASpaceWithDevicesIsRejected() {
        // Business / User Story Rational (WS-US-32): no se puede eliminar un espacio que tiene sensores activos asignados.
        // Arrange
        Space space = new Space("Sala", UUID.randomUUID(), OWNER);
        SpaceRepository spaces = mock(SpaceRepository.class);
        DeviceAssignmentRepository assignments = mock(DeviceAssignmentRepository.class);
        when(spaces.findById(space.getId())).thenReturn(Optional.of(space));
        when(assignments.existsBySpaceId(space.getId())).thenReturn(true);
        var service = new SpaceCommandServiceImpl(spaces, mock(OrganizationRepository.class), assignments,
                mock(ExternalBillingService.class));

        // Act + Assert
        assertThatThrownBy(() -> service.handle(new DeleteSpaceCommand(space.getId(), OWNER)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot delete space with devices. Remove all devices first.");
        verify(spaces, never()).deleteById(any());
    }

    @Test
    @DisplayName("Límite inferior: un nombre de organización de un solo carácter es aceptado")
    void oneCharacterOrganizationNameIsAccepted() {
        // Business / User Story Rational (WS-US-24): basta un nombre no vacío para identificar la organización.
        // Arrange
        String shortestName = "A";

        // Act
        CreateOrganizationCommand command = new CreateOrganizationCommand(shortestName, OWNER);

        // Assert
        assertThat(command.name()).isEqualTo("A");
    }

    @Test
    @DisplayName("Datos insuficientes: crear una organización sin nombre es rechazado")
    void creatingAnOrganizationWithoutNameIsRejected() {
        // Business / User Story Rational (WS-US-24): toda organización necesita un nombre visible en la app.
        // Arrange
        String blankName = " ";

        // Act + Assert
        assertThatThrownBy(() -> new CreateOrganizationCommand(blankName, OWNER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Organization name must not be null or blank");
    }

    @Test
    @DisplayName("Estado inválido: un espacio sin organización no puede existir")
    void aSpaceWithoutOrganizationIsRejected() {
        // Business / User Story Rational (WS-US-29): todo espacio pertenece a una organización.
        // Arrange
        UUID missingOrganization = null;

        // Act + Assert
        assertThatThrownBy(() -> new Space("Sala de reuniones", missingOrganization, OWNER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Organization ID must not be null");
    }

    @Test
    @DisplayName("Estado inválido: crear un espacio sin dueño es rechazado")
    void creatingASpaceWithoutOwnerIsRejected() {
        // Business / User Story Rational (WS-US-29): el dueño define quién puede ver y administrar el espacio.
        // Arrange
        UUID organizationId = UUID.randomUUID();

        // Act + Assert
        assertThatThrownBy(() -> new CreateSpaceCommand("Sala", organizationId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Owner user ID must not be null");
    }

    @Test
    @DisplayName("Condicional A: renombrar un espacio con un nombre válido actualiza solo el nombre")
    void renamingASpaceOnlyChangesItsName() {
        // Business / User Story Rational (WS-US-33): corregir la etiqueta de un espacio no cambia a qué organización pertenece.
        // Arrange
        UUID organizationId = UUID.randomUUID();
        Space space = new Space("Sala 1", organizationId, OWNER);
        UpdateSpaceNameCommand command = new UpdateSpaceNameCommand(space.getId(), "Sala de directorio", OWNER);

        // Act
        space.updateName(command.name());

        // Assert
        assertThat(space.getName()).isEqualTo("Sala de directorio");
        assertThat(space.getOrganizationId()).isEqualTo(organizationId);
        assertThat(space.getOwnerUserId()).isEqualTo(OWNER);
    }

    @Test
    @DisplayName("Condicional B: un nombre en blanco para el espacio se rechaza antes de llegar al agregado")
    void renamingASpaceWithBlankNameIsRejected() {
        // Business / User Story Rational (WS-US-33): un espacio nunca debe quedar sin etiqueta.
        // Arrange
        UUID spaceId = UUID.randomUUID();

        // Act + Assert
        assertThatThrownBy(() -> new UpdateSpaceNameCommand(spaceId, "", OWNER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Name must not be null or blank");
    }

    @Test
    @DisplayName("Integridad: dos espacios de la misma organización tienen identidades distintas")
    void spacesOfTheSameOrganizationHaveDistinctIds() {
        // Business / User Story Rational (WS-US-31): listar los espacios de una organización exige que cada uno sea único.
        // Arrange
        UUID organizationId = UUID.randomUUID();

        // Act
        Space first = new Space("Sala 1", organizationId, OWNER);
        Space second = new Space("Sala 2", organizationId, OWNER);

        // Assert
        assertThat(first.getId()).isNotEqualTo(second.getId());
        assertThat(first.getOrganizationId()).isEqualTo(second.getOrganizationId());
    }
}
