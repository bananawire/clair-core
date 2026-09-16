package com.claircore.device.application.acl;

import com.claircore.device.domain.model.queries.GetDeviceByApiKeyQuery;
import com.claircore.device.domain.model.queries.GetDeviceByIdQuery;
import com.claircore.device.domain.model.queries.GetDeviceByHardwareIdQuery;
import com.claircore.device.domain.model.queries.GetSpaceByIdQuery;
import com.claircore.device.domain.model.queries.GetOrganizationsByOwnerQuery;
import com.claircore.device.domain.model.queries.GetSpacesByOrganizationQuery;
import com.claircore.device.domain.model.queries.GetDevicesBySpaceQuery;
import com.claircore.device.domain.model.queries.GetDevicesForTelemetryQuery;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.application.queryservices.DeviceQueryService;
import com.claircore.device.application.queryservices.DeviceCommandQueryService;
import com.claircore.device.application.commandservices.DeviceControlCommandService;
import com.claircore.device.domain.model.commands.AcknowledgeDeviceCommandCommand;
import com.claircore.device.domain.model.valueobjects.DeviceCommandStatus;
import com.claircore.device.interfaces.acl.DeviceContextFacade;
import com.claircore.device.interfaces.acl.OrganizationSummary;
import com.claircore.device.interfaces.acl.SpaceSummary;
import com.claircore.device.interfaces.acl.DeviceTelemetryTarget;
import com.claircore.device.interfaces.acl.DeviceCommandForEdge;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class DeviceContextFacadeImpl implements DeviceContextFacade {

    private final DeviceQueryService deviceQueryService;
    private final DeviceCommandQueryService deviceCommandQueryService;
    private final DeviceControlCommandService deviceControlCommandService;

    public DeviceContextFacadeImpl(DeviceQueryService deviceQueryService) {
        this(deviceQueryService, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public DeviceContextFacadeImpl(DeviceQueryService deviceQueryService,
                                   DeviceCommandQueryService deviceCommandQueryService,
                                   DeviceControlCommandService deviceControlCommandService) {
        this.deviceQueryService = deviceQueryService;
        this.deviceCommandQueryService = deviceCommandQueryService;
        this.deviceControlCommandService = deviceControlCommandService;
    }

    @Override
    public Optional<UUID> findDeviceIdByApiKey(String apiKey) {
        var query = new GetDeviceByApiKeyQuery(apiKey);
        return deviceQueryService.handle(query)
                .map(device -> device.getId());
    }

    @Override
    public Optional<UUID> findDeviceIdByHardwareId(String hardwareId) {
        var query = new GetDeviceByHardwareIdQuery(hardwareId);
        return deviceQueryService.handle(query)
                .map(device -> device.getId());
    }

    @Override
    public Optional<UUID> findSpaceIdByDeviceId(UUID deviceId) {
        return deviceQueryService.findSpaceIdByDeviceId(deviceId);
    }

    @Override
    public Optional<String> findHardwareIdByDeviceId(UUID deviceId) {
        var query = new GetDeviceByIdQuery(deviceId);
        return deviceQueryService.handle(query)
                .map(device -> device.getHardwareId().value());
    }

    @Override
    public boolean isDeviceOwnedByUser(UUID deviceId, UUID userId) {
        return deviceQueryService.isDeviceOwnedByUser(deviceId, userId);
    }

    @Override
    public Optional<java.time.Instant> findVisibleSinceByDeviceId(UUID deviceId) {
        return deviceQueryService.findActivatedAtByDeviceId(deviceId);
    }

    @Override
    public Optional<UUID> findOwnerIdByDeviceId(UUID deviceId) {
        return deviceQueryService.findOwnerIdByDeviceId(deviceId);
    }


    @Override
    public boolean isSpaceOwnedByUser(UUID spaceId, UUID userId) {
        return deviceQueryService.isSpaceOwnedByUser(spaceId, userId);
    }

    @Override
    public List<UUID> findDeviceIdsByOwnerId(UUID ownerUserId) {
        return deviceQueryService.findDeviceIdsByOwnerId(ownerUserId);
    }

    @Override
    public Optional<String> findSpaceNameBySpaceId(UUID spaceId) {
        var query = new GetSpaceByIdQuery(spaceId);
        return deviceQueryService.handle(query)
                .map(space -> space.getName());
    }

    @Override
    public Optional<String> findDeviceNameByDeviceId(UUID deviceId) {
        var query = new GetDeviceByIdQuery(deviceId);
        return deviceQueryService.handle(query)
                .map(device -> device.getName());
    }

    @Override
    public Map<UUID, String> findDeviceNamesByDeviceIds(List<UUID> deviceIds) {
        return deviceQueryService.findDeviceNamesByDeviceIds(deviceIds);
    }

    @Override
    public Map<UUID, String> findSpaceNamesBySpaceIds(List<UUID> spaceIds) {
        return deviceQueryService.findSpaceNamesBySpaceIds(spaceIds);
    }

    @Override
    public Map<UUID, String> findHardwareIdsByDeviceIds(List<UUID> deviceIds) {
        return deviceQueryService.findHardwareIdsByDeviceIds(deviceIds);
    }

    @Override
    public List<OrganizationSummary> findOrganizationsByOwnerId(UUID ownerUserId) {
        var query = new GetOrganizationsByOwnerQuery(new UserId(ownerUserId));
        return deviceQueryService.handle(query)
                .stream()
                .map(o -> new OrganizationSummary(o.getId(), o.getName()))
                .toList();
    }

    @Override
    public List<SpaceSummary> findSpacesByOrganizationId(UUID organizationId) {
        var query = new GetSpacesByOrganizationQuery(organizationId);
        return deviceQueryService.handle(query)
                .stream()
                .map(s -> new SpaceSummary(s.getId(), s.getName(), s.getOrganizationId()))
                .toList();
    }

    @Override
    public List<UUID> findDeviceIdsBySpaceId(UUID spaceId, int limit) {
        int size = limit > 0 ? limit : 200;
        return deviceQueryService.handle(new GetDevicesBySpaceQuery(spaceId, 0, size)).items().stream()
                .map(assigned -> assigned.device().getId())
                .toList();
    }

    @Override
    public List<DeviceTelemetryTarget> findTelemetryTargets(int limit, boolean includeDeleted) {
        int size = limit > 0 ? Math.min(limit, 500) : 50;
        var page = deviceQueryService.handle(new GetDevicesForTelemetryQuery(size, includeDeleted));
        return page.items().stream().map(device -> {
            var assignment = deviceQueryService.findAssignmentByDeviceId(device.getId());
            return new DeviceTelemetryTarget(device.getId(), device.getHardwareId().value(),
                    device.getName(), assignment.isPresent(),
                    assignment.map(DeviceAssignment::getStatus).map(status -> status.name()).orElse(null));
        }).toList();
    }

    @Override
    public List<DeviceCommandForEdge> findClaimableCommands(java.time.Instant leaseCutoff, int limit) {
        return deviceCommandQueryService.findClaimableForEdge(leaseCutoff, limit).stream()
                .map(DeviceContextFacadeImpl::toEdgeCommand).toList();
    }

    @Override
    public Optional<DeviceCommandForEdge> claimCommand(UUID commandId, java.time.Instant leaseCutoff,
                                                        java.time.Instant claimedAt) {
        return deviceControlCommandService.claimForEdge(commandId, leaseCutoff, claimedAt)
                .map(DeviceContextFacadeImpl::toEdgeCommand);
    }

    @Override
    public Optional<DeviceCommandForEdge> acknowledgeCommand(UUID deviceId, UUID commandId,
                                                               String status, String failureReason) {
        DeviceCommandStatus parsed;
        try { parsed = DeviceCommandStatus.valueOf(status); }
        catch (IllegalArgumentException | NullPointerException ex) {
            throw new IllegalArgumentException("Unknown command ACK status: " + status, ex);
        }
        return Optional.of(toEdgeCommand(deviceControlCommandService.handle(
                new AcknowledgeDeviceCommandCommand(deviceId, commandId, parsed, failureReason))));
    }

    private static DeviceCommandForEdge toEdgeCommand(com.claircore.device.domain.model.aggregates.DeviceCommand command) {
        return new DeviceCommandForEdge(command.getId(), command.getDeviceId(), command.getAssignmentId(),
                command.getType().name(), command.getPayload(), command.getSentAt());
    }

    @Override
    public void recordDevicePresence(UUID deviceId, String status, java.time.Instant occurredAt) {
        com.claircore.device.domain.model.valueobjects.DeviceStatus parsed;
        try {
            parsed = com.claircore.device.domain.model.valueobjects.DeviceStatus.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new IllegalArgumentException("Unknown device status: " + status, ex);
        }
        java.time.Instant occurred = occurredAt != null ? occurredAt : java.time.Instant.now();
        deviceQueryService.updatePresence(deviceId, parsed, occurred);
    }
}
