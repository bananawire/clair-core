package com.claircore.device.application.internal.commandservices;

import com.claircore.device.application.internal.outboundservices.acl.ExternalBillingService;
import com.claircore.device.domain.model.commands.ClaimDeviceCommand;
import com.claircore.device.domain.model.commands.PairDeviceCommand;
import com.claircore.device.domain.model.commands.ResetDeviceAssignmentCommand;
import com.claircore.device.domain.model.commands.ImportDevicesCommand;
import com.claircore.device.domain.model.commands.SeedDevicesCommand;
import com.claircore.device.domain.model.aggregates.Device;
import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.aggregates.Space;
import com.claircore.device.domain.model.valueobjects.ClaimToken;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.domain.repositories.DeviceAssignmentRepository;
import com.claircore.device.domain.repositories.DeviceRepository;
import com.claircore.device.domain.repositories.OrganizationRepository;
import com.claircore.device.domain.repositories.SpaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceCommandServiceImplTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceAssignmentRepository deviceAssignmentRepository;

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ExternalBillingService externalBillingService;
    @Mock
    private com.claircore.device.domain.repositories.DeviceCommandRepository deviceCommandRepository;

    @InjectMocks
    private DeviceCommandServiceImpl service;

    @Test
    void seedDevicesCreatesNonExistingOnes() {
        when(deviceRepository.existsByHardwareId(any())).thenReturn(false);
        when(deviceRepository.save(any(Device.class))).thenAnswer(i -> {
            Device device = i.getArgument(0);
            // In production JPA assigns the id; in this unit test we do it manually.
            ReflectionTestUtils.setField(device, "id", UUID.randomUUID());
            return device;
        });

        List<Device> result = service.handle(new SeedDevicesCommand(2));

        assertEquals(2, result.size());
        verify(deviceRepository, times(2)).save(any(Device.class));
    }

    @Test
    void importSkipsRowsWhoseSerialOrHardwareIdAlreadyExist() {
        var existingSerial = new ImportDevicesCommand.DeviceProvisioningRecord("SN-0001", "CLAIR-0001", "k1", "Sensor 0001");
        var existingHardware = new ImportDevicesCommand.DeviceProvisioningRecord("SN-0002", "CLAIR-0002", "k2", "Sensor 0002");
        var fresh = new ImportDevicesCommand.DeviceProvisioningRecord("SN-0003", "CLAIR-0003", "k3", "Sensor 0003");
        when(deviceRepository.findBySerialNumber("SN-0001")).thenReturn(Optional.of(deviceWithId(UUID.randomUUID(), "SN-0001", "CLAIR-0001")));
        when(deviceRepository.findBySerialNumber("SN-0002")).thenReturn(Optional.empty());
        when(deviceRepository.existsByHardwareId("CLAIR-0002")).thenReturn(true);
        when(deviceRepository.findBySerialNumber("SN-0003")).thenReturn(Optional.empty());
        when(deviceRepository.existsByHardwareId("CLAIR-0003")).thenReturn(false);
        when(deviceRepository.save(any(Device.class))).thenAnswer(i -> {
            Device device = i.getArgument(0);
            ReflectionTestUtils.setField(device, "id", UUID.randomUUID());
            return device;
        });
        List<Device> created = service.handle(new ImportDevicesCommand(List.of(existingSerial, existingHardware, fresh)));
        assertEquals(1, created.size());
        assertEquals("CLAIR-0003", created.getFirst().getHardwareId().value());
        assertEquals("k3", created.getFirst().getApiKey().value());
        verify(deviceRepository, times(1)).save(any(Device.class));
    }

    @Test
    void pairDeviceFailsWhenHardwareNotFoundInFactoryInventory() {
        when(deviceRepository.findByHardwareIdForUpdate("HW-0001")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            service.handle(new PairDeviceCommand("HW-0001"))
        );

        verify(deviceAssignmentRepository, never()).save(any(DeviceAssignment.class));
    }

    @Test
    void pairDeviceCreatesAssignmentForFactoryDeviceWithoutAssignment() {
        Device existing = deviceWithId(UUID.randomUUID(), "SN-001", "HW-0001");
        when(deviceRepository.findByHardwareIdForUpdate("HW-0001")).thenReturn(Optional.of(existing));
        when(deviceAssignmentRepository.findByDeviceIdForUpdate(existing.getId())).thenReturn(Optional.empty());
        when(deviceAssignmentRepository.save(any(DeviceAssignment.class))).thenAnswer(i -> i.getArgument(0));
        when(deviceRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        DeviceAssignment result = service.handle(new PairDeviceCommand("HW-0001"));

        assertEquals(existing.getId(), result.getDeviceId());
        verify(deviceRepository, never()).save(any(Device.class));
        verify(deviceAssignmentRepository).save(any(DeviceAssignment.class));
    }

    @Test
    void pairDeviceFailsWhenAlreadyPaired() {
        Device existing = deviceWithId(UUID.randomUUID(), "SN-001", "HW-0001");
        DeviceAssignment assignment = new DeviceAssignment(existing.getId(), ClaimToken.generate());
        assignment.claimToSpace(UUID.randomUUID(), new UserId(UUID.randomUUID()));
        when(deviceRepository.findByHardwareIdForUpdate("HW-0001")).thenReturn(Optional.of(existing));
        when(deviceAssignmentRepository.findByDeviceIdForUpdate(existing.getId())).thenReturn(Optional.of(assignment));

        assertThrows(IllegalStateException.class, () ->
            service.handle(new PairDeviceCommand("HW-0001"))
        );
    }

    @Test
    void claimDeviceAssignsDeviceToUserOwnedSpace() {
        UUID userId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        Device device = deviceWithId(UUID.randomUUID(), "SN-002", "HW-0002");
        DeviceAssignment assignment = new DeviceAssignment(device.getId(), ClaimToken.generate());
        Space space = spaceWithId(spaceId, userId);

        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));
        when(deviceAssignmentRepository.findByClaimTokenForUpdate(assignment.getClaimToken().value())).thenReturn(Optional.of(assignment));
        when(deviceAssignmentRepository.countByOwnerUserId(new UserId(userId))).thenReturn(0L);
        when(externalBillingService.getMaxDevices(userId)).thenReturn(3);
        when(deviceAssignmentRepository.save(any(DeviceAssignment.class))).thenAnswer(i -> i.getArgument(0));
        when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));

        DeviceAssignment result = service.handle(new ClaimDeviceCommand(
            assignment.getClaimToken().value(),
            spaceId,
            new UserId(userId)
        ));

        assertEquals(spaceId, result.getSpaceId());
        assertEquals(new UserId(userId), result.getOwnerUserId());
        assertNotNull(result.getActivatedAt());
    }

    @Test
    void claimDeviceFailsWhenSpaceBelongsToAnotherUser() {
        UUID userId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        Space space = spaceWithId(spaceId, anotherUserId);

        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));

        assertThrows(AccessDeniedException.class, () ->
            service.handle(new ClaimDeviceCommand("claim-token", spaceId, new UserId(userId)))
        );
        verify(deviceAssignmentRepository, never()).findByClaimTokenForUpdate(any());
    }

    @Test
    void claimAtQuotaFailsBeforeConsumingTheToken() {
        UUID userId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        Device device = deviceWithId(UUID.randomUUID(), "SN-002", "HW-0002");
        DeviceAssignment assignment = new DeviceAssignment(device.getId(), ClaimToken.generate());
        String token = assignment.getClaimToken().value();
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(spaceWithId(spaceId, userId)));
        when(deviceAssignmentRepository.findByClaimTokenForUpdate(token)).thenReturn(Optional.of(assignment));
        when(deviceAssignmentRepository.countByOwnerUserId(new UserId(userId))).thenReturn(3L);
        when(externalBillingService.getMaxDevices(userId)).thenReturn(3);
        assertThrows(IllegalStateException.class, () ->
            service.handle(new ClaimDeviceCommand(token, spaceId, new UserId(userId))));
        // Nothing was written and the aggregate still carries its token, so a retry after
        // freeing a slot can succeed.
        verify(deviceAssignmentRepository, never()).save(any(DeviceAssignment.class));
        assertEquals(token, assignment.getClaimToken().value());
        org.mockito.InOrder order = org.mockito.Mockito.inOrder(deviceAssignmentRepository);
        order.verify(deviceAssignmentRepository).lockOwnerQuotaBoundary(new UserId(userId));
        order.verify(deviceAssignmentRepository).countByOwnerUserId(new UserId(userId));
    }

    @Test
    void deleteDeviceDeletesAssignmentForOwnerOnly() {
        UUID userId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        Device device = deviceWithId(deviceId, "SN-003", "HW-0003");
        DeviceAssignment assignment = new DeviceAssignment(device.getId(), ClaimToken.generate());
        assignment.claimToSpace(spaceId, new UserId(userId));

        when(deviceAssignmentRepository.findByDeviceIdForUpdate(deviceId)).thenReturn(Optional.of(assignment));
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

        service.handle(new ResetDeviceAssignmentCommand(deviceId, new UserId(userId)));

        verify(deviceAssignmentRepository).deleteById(assignment.getId());
        // Everything still queued for the old owner is voided in the same transaction, before the
        // assignment row goes, so nothing can claim or acknowledge it afterwards.
        org.mockito.InOrder order = org.mockito.Mockito.inOrder(deviceCommandRepository, deviceAssignmentRepository);
        order.verify(deviceCommandRepository).expireOutstandingByAssignmentId(assignment.getId());
        order.verify(deviceAssignmentRepository).deleteById(assignment.getId());
        // The device row is never removed; a reset unlinks it, a decommission tombstones it.
        org.junit.jupiter.api.Assertions.assertFalse(device.isDeleted());
    }

    private Device deviceWithId(UUID deviceId, String serialNumber, String hardwareId) {
        Device device = new Device(
            serialNumber,
            "Sensor",
            new com.claircore.device.domain.model.valueobjects.HardwareId(hardwareId),
            com.claircore.device.domain.model.valueobjects.ApiKey.generate(),
            new com.claircore.device.domain.model.valueobjects.DeviceType("air-quality-v1")
        );
        ReflectionTestUtils.setField(device, "id", deviceId);
        return device;
    }

    private Space spaceWithId(UUID spaceId, UUID ownerUserId) {
        Space space = new Space("Living Room", UUID.randomUUID(), new UserId(ownerUserId));
        ReflectionTestUtils.setField(space, "id", spaceId);
        return space;
    }
}
