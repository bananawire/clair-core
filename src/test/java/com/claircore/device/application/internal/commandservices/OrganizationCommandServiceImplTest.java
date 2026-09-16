package com.claircore.device.application.internal.commandservices;

import com.claircore.device.application.internal.outboundservices.acl.ExternalBillingService;
import com.claircore.device.domain.model.commands.DeleteOrganizationCommand;
import com.claircore.device.domain.model.commands.UpdateOrganizationNameCommand;
import com.claircore.device.domain.model.aggregates.Organization;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.domain.repositories.DeviceAssignmentRepository;
import com.claircore.device.domain.repositories.OrganizationRepository;
import com.claircore.device.domain.repositories.SpaceRepository;
import org.springframework.security.access.AccessDeniedException;
import static org.mockito.ArgumentMatchers.any;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationCommandServiceImplTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private DeviceAssignmentRepository deviceAssignmentRepository;

    @Mock
    private ExternalBillingService externalBillingService;

    @InjectMocks
    private OrganizationCommandServiceImpl service;

    @Test
    void deleteOrganizationFailsWhenOrganizationHasRegisteredDevices() {
        UUID organizationId = UUID.randomUUID();
        UserId owner = new UserId(UUID.randomUUID());
        Organization organization = new Organization("Home", owner);
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(deviceAssignmentRepository.existsByOrganizationId(organizationId)).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> service.handle(new DeleteOrganizationCommand(organizationId, owner)));
        verify(spaceRepository, never()).deleteByOrganizationId(organizationId);
        verify(organizationRepository, never()).deleteById(organization.getId());
    }

    @Test
    void deleteOrganizationSucceedsWhenOrganizationHasNoRegisteredDevices() {
        UUID organizationId = UUID.randomUUID();
        UserId owner = new UserId(UUID.randomUUID());
        Organization organization = new Organization("Home", owner);
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(deviceAssignmentRepository.existsByOrganizationId(organizationId)).thenReturn(false);
        service.handle(new DeleteOrganizationCommand(organizationId, owner));
        verify(spaceRepository).deleteByOrganizationId(organizationId);
        verify(organizationRepository).deleteById(organization.getId());
    }

    @Test
    void deleteOrganizationIsRefusedForAnotherUser() {
        UUID organizationId = UUID.randomUUID();
        Organization organization = new Organization("Home", new UserId(UUID.randomUUID()));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        assertThrows(AccessDeniedException.class,
                () -> service.handle(new DeleteOrganizationCommand(organizationId, new UserId(UUID.randomUUID()))));
        verify(spaceRepository, never()).deleteByOrganizationId(any());
        verify(organizationRepository, never()).deleteById(any());
    }

    @Test
    void updateOrganizationNameSucceedsForOwner() {
        UUID organizationId = UUID.randomUUID();
        UserId owner = new UserId(UUID.randomUUID());
        Organization organization = new Organization("Home", owner);
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        service.handle(new UpdateOrganizationNameCommand(organizationId, "Office", owner));
        verify(organizationRepository).save(organization);
    }

    @Test
    void updateOrganizationNameIsRefusedForAnotherUser() {
        UUID organizationId = UUID.randomUUID();
        Organization organization = new Organization("Home", new UserId(UUID.randomUUID()));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        assertThrows(AccessDeniedException.class,
                () -> service.handle(new UpdateOrganizationNameCommand(organizationId, "Office", new UserId(UUID.randomUUID()))));
        verify(organizationRepository, never()).save(any());
    }
}
