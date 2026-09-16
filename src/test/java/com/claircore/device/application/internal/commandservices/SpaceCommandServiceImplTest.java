package com.claircore.device.application.internal.commandservices;

import com.claircore.device.application.internal.outboundservices.acl.ExternalBillingService;
import com.claircore.device.domain.model.commands.DeleteSpaceCommand;
import com.claircore.device.domain.model.commands.UpdateSpaceNameCommand;
import com.claircore.device.domain.model.aggregates.Space;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.domain.repositories.DeviceAssignmentRepository;
import com.claircore.device.domain.repositories.OrganizationRepository;
import com.claircore.device.domain.repositories.SpaceRepository;
import com.claircore.device.domain.model.aggregates.Organization;
import com.claircore.device.domain.model.commands.CreateSpaceCommand;
import org.springframework.security.access.AccessDeniedException;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
class SpaceCommandServiceImplTest {

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private DeviceAssignmentRepository deviceAssignmentRepository;

    @Mock
    private ExternalBillingService externalBillingService;

    @InjectMocks
    private SpaceCommandServiceImpl service;

    @Test
    void deleteSpaceFailsWhenSpaceHasRegisteredDevices() {
        UUID spaceId = UUID.randomUUID();
        UserId owner = new UserId(UUID.randomUUID());
        Space space = new Space("Living Room", UUID.randomUUID(), owner);
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));
        when(deviceAssignmentRepository.existsBySpaceId(spaceId)).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> service.handle(new DeleteSpaceCommand(spaceId, owner)));
        verify(spaceRepository, never()).deleteById(space.getId());
    }

    @Test
    void deleteSpaceSucceedsWhenSpaceHasNoRegisteredDevices() {
        UUID spaceId = UUID.randomUUID();
        UserId owner = new UserId(UUID.randomUUID());
        Space space = new Space("Living Room", UUID.randomUUID(), owner);
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));
        when(deviceAssignmentRepository.existsBySpaceId(spaceId)).thenReturn(false);
        service.handle(new DeleteSpaceCommand(spaceId, owner));
        verify(spaceRepository).deleteById(space.getId());
    }

    @Test
    void deleteSpaceIsRefusedForAnotherUserBeforeAnyDeviceCheck() {
        UUID spaceId = UUID.randomUUID();
        Space space = new Space("Living Room", UUID.randomUUID(), new UserId(UUID.randomUUID()));
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));
        assertThrows(AccessDeniedException.class,
                () -> service.handle(new DeleteSpaceCommand(spaceId, new UserId(UUID.randomUUID()))));
        verify(deviceAssignmentRepository, never()).existsBySpaceId(any());
        verify(spaceRepository, never()).deleteById(any());
    }

    @Test
    void updateSpaceNameSucceedsForOwner() {
        UUID spaceId = UUID.randomUUID();
        UserId owner = new UserId(UUID.randomUUID());
        Space space = new Space("Living Room", UUID.randomUUID(), owner);
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));
        service.handle(new UpdateSpaceNameCommand(spaceId, "Kitchen", owner));
        verify(spaceRepository).save(space);
    }

    @Test
    void updateSpaceNameIsRefusedForAnotherUser() {
        UUID spaceId = UUID.randomUUID();
        Space space = new Space("Living Room", UUID.randomUUID(), new UserId(UUID.randomUUID()));
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));
        assertThrows(AccessDeniedException.class,
                () -> service.handle(new UpdateSpaceNameCommand(spaceId, "Kitchen", new UserId(UUID.randomUUID()))));
        verify(spaceRepository, never()).save(any());
        assertEquals("Living Room", space.getName());
    }

    @Test
    void createSpaceIsRefusedUnderAnotherUsersOrganization() {
        UUID organizationId = UUID.randomUUID();
        Organization organization = new Organization("Home", new UserId(UUID.randomUUID()));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        assertThrows(AccessDeniedException.class,
                () -> service.handle(new CreateSpaceCommand("Kitchen", organizationId, new UserId(UUID.randomUUID()))));
        verify(externalBillingService, never()).getMaxSpaces(any());
        verify(spaceRepository, never()).save(any());
    }

    @Test
    void createSpaceSucceedsForOrganizationOwner() {
        UUID organizationId = UUID.randomUUID();
        UserId owner = new UserId(UUID.randomUUID());
        Organization organization = new Organization("Home", owner);
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(spaceRepository.countByOwnerUserId(owner)).thenReturn(0);
        when(externalBillingService.getMaxSpaces(owner.userId())).thenReturn(5);
        when(spaceRepository.save(any(Space.class))).thenAnswer(i -> i.getArgument(0));
        Space created = service.handle(new CreateSpaceCommand("Kitchen", organizationId, owner));
        assertEquals(organizationId, created.getOrganizationId());
        assertEquals(owner, created.getOwnerUserId());
    }
}
