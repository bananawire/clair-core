package com.claircore.device.application.internal.queryservices;

import com.claircore.device.domain.model.aggregates.Device;
import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.aggregates.Organization;
import com.claircore.device.domain.model.aggregates.Space;
import com.claircore.device.domain.model.queries.GetDevicesBySpaceQuery;
import com.claircore.device.domain.model.queries.GetProvisionedDevicesQuery;
import com.claircore.device.domain.model.valueobjects.ApiKey;
import com.claircore.device.domain.model.valueobjects.ClaimToken;
import com.claircore.device.domain.model.valueobjects.DeviceType;
import com.claircore.device.domain.model.valueobjects.HardwareId;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.domain.repositories.DeviceAssignmentRepository;
import com.claircore.device.domain.repositories.DeviceRepository;
import com.claircore.device.domain.repositories.OrganizationRepository;
import com.claircore.device.domain.repositories.SpaceRepository;
import com.claircore.device.domain.model.queries.GetSpaceByIdForUserQuery;
import com.claircore.device.domain.model.queries.GetSpacesByOrganizationForUserQuery;
import com.claircore.device.domain.model.queries.GetOrganizationByIdForUserQuery;
import com.claircore.device.domain.model.queries.GetDevicesBySpaceForUserQuery;
import com.claircore.device.domain.model.queries.GetAssignedDeviceByIdForUserQuery;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceQueryServiceImplTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceAssignmentRepository deviceAssignmentRepository;

    @Test
    void shouldReturnPagedAssignmentsWhenListingDevicesBySpace() {
        DeviceQueryServiceImpl service = new DeviceQueryServiceImpl(
                organizationRepository,
                spaceRepository,
                deviceRepository,
                deviceAssignmentRepository
        );

        UUID spaceId = UUID.fromString("550e8400-e29b-41d4-a716-446655440020");
        var assignment = assignmentFor(spaceId);
        var device = deviceFor(assignment.getDeviceId());
        when(deviceAssignmentRepository.findBySpaceId(spaceId, 2, 15))
                .thenReturn(new com.claircore.shared.domain.model.PageResult<>(List.of(assignment), 2, 15, 1));
        when(deviceRepository.findAllById(List.of(assignment.getDeviceId()))).thenReturn(List.of(device));

        var result = service.handle(new GetDevicesBySpaceQuery(spaceId, 2, 15));

        assertEquals(1, result.total());
        assertEquals(device.getId(), result.items().getFirst().device().getId());
        verify(deviceAssignmentRepository).findBySpaceId(spaceId, 2, 15);
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void shouldCapProvisionedDeviceLimitWhenQueryRequestsTooMany() {
        DeviceQueryServiceImpl service = new DeviceQueryServiceImpl(
                organizationRepository,
                spaceRepository,
                deviceRepository,
                deviceAssignmentRepository
        );

        when(deviceRepository.findProvisionedDevices(isNull(), isNull(), anyInt()))
                .thenReturn(com.claircore.shared.domain.model.PageResult.empty(0, 5000));

        service.handle(new GetProvisionedDevicesQuery(10_000));

        ArgumentCaptor<Integer> limitCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(deviceRepository).findProvisionedDevices(isNull(), isNull(), limitCaptor.capture());
        assertEquals(5000, limitCaptor.getValue());
    }

    @Test
    void shouldReturnFalseWhenDeviceOwnershipCheckReceivesNullArguments() {
        DeviceQueryServiceImpl service = new DeviceQueryServiceImpl(
                organizationRepository,
                spaceRepository,
                deviceRepository,
                deviceAssignmentRepository
        );

        assertFalse(service.isDeviceOwnedByUser(null, UUID.randomUUID()));
        assertFalse(service.isDeviceOwnedByUser(UUID.randomUUID(), null));
        verify(deviceAssignmentRepository, never()).existsByDeviceIdAndOwnerUserId(any(), any());
    }

    @Test
    void shouldReturnTrueWhenSpaceOwnershipCheckMatchesRepository() {
        DeviceQueryServiceImpl service = new DeviceQueryServiceImpl(
                organizationRepository,
                spaceRepository,
                deviceRepository,
                deviceAssignmentRepository
        );

        UUID spaceId = UUID.fromString("550e8400-e29b-41d4-a716-446655440030");
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440031");
        when(spaceRepository.existsByIdAndOwnerUserId(spaceId, new UserId(userId))).thenReturn(true);

        assertTrue(service.isSpaceOwnedByUser(spaceId, userId));
        verify(spaceRepository).existsByIdAndOwnerUserId(spaceId, new UserId(userId));
    }

    private DeviceAssignment assignmentFor(UUID spaceId) {
        Device device = new Device(
                "SN-0001",
                "Sensor 0001",
                new HardwareId("CLAIR-0KBG"),
                ApiKey.generate(),
                new DeviceType("air-quality-v1")
        );
        DeviceAssignment assignment = new DeviceAssignment(device.getId(), ClaimToken.generate());
        assignment.claimToSpace(spaceId, new UserId(UUID.fromString("550e8400-e29b-41d4-a716-446655440040")));
        return assignment;
    }

    private Device deviceFor(UUID deviceId) {
        return Device.reconstitute(deviceId, "SN-0001", "Sensor 0001", "Sensor 0001", false,
                new HardwareId("CLAIR-0KBG"), ApiKey.generate(), new DeviceType("air-quality-v1"),
                null, null);
    }

    @Test
    void anotherUserCannotReadASpaceByIdOrListItsDevices() {
        DeviceQueryServiceImpl service = new DeviceQueryServiceImpl(
                organizationRepository, spaceRepository, deviceRepository, deviceAssignmentRepository);
        UUID spaceId = UUID.randomUUID();
        UserId owner = new UserId(UUID.randomUUID());
        UserId intruder = new UserId(UUID.randomUUID());
        Space space = Space.reconstitute(spaceId, "Kitchen", UUID.randomUUID(), owner, null, null);
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));
        when(spaceRepository.existsByIdAndOwnerUserId(spaceId, intruder)).thenReturn(false);

        assertTrue(service.handle(new GetSpaceByIdForUserQuery(spaceId, intruder)).isEmpty());
        assertTrue(service.handle(new GetSpaceByIdForUserQuery(spaceId, owner)).isPresent());
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.handle(new GetDevicesBySpaceForUserQuery(spaceId, 0, 20, intruder)));
        verify(deviceAssignmentRepository, never()).findBySpaceId(any(), anyInt(), anyInt());
    }

    @Test
    void anotherUserCannotListSpacesOfAnOrganizationOrReadItById() {
        DeviceQueryServiceImpl service = new DeviceQueryServiceImpl(
                organizationRepository, spaceRepository, deviceRepository, deviceAssignmentRepository);
        UUID organizationId = UUID.randomUUID();
        UserId owner = new UserId(UUID.randomUUID());
        UserId intruder = new UserId(UUID.randomUUID());
        Organization organization = Organization.reconstitute(organizationId, "Home", owner, null, null);
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.handle(new GetSpacesByOrganizationForUserQuery(organizationId, intruder)));
        verify(spaceRepository, never()).findByOrganizationId(any());
        assertTrue(service.handle(new GetOrganizationByIdForUserQuery(organizationId, intruder)).isEmpty());
        assertTrue(service.handle(new GetOrganizationByIdForUserQuery(organizationId, owner)).isPresent());
    }

    @Test
    void anotherUserCannotReadAClaimedDeviceById() {
        DeviceQueryServiceImpl service = new DeviceQueryServiceImpl(
                organizationRepository, spaceRepository, deviceRepository, deviceAssignmentRepository);
        UserId owner = new UserId(UUID.randomUUID());
        var assignment = new DeviceAssignment(UUID.randomUUID(), ClaimToken.generate());
        assignment.claimToSpace(UUID.randomUUID(), owner);
        var device = deviceFor(assignment.getDeviceId());
        when(deviceAssignmentRepository.findByDeviceId(device.getId())).thenReturn(Optional.of(assignment));
        when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));

        assertTrue(service.handle(new GetAssignedDeviceByIdForUserQuery(device.getId(), new UserId(UUID.randomUUID()))).isEmpty());
        assertTrue(service.handle(new GetAssignedDeviceByIdForUserQuery(device.getId(), owner)).isPresent());
    }
}
