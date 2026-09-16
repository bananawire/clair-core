package com.claircore.device.application.internal.queryservices;

import com.claircore.device.domain.model.aggregates.DeviceCommand;
import com.claircore.device.domain.model.queries.GetDeviceCommandByIdForUserQuery;
import com.claircore.device.domain.model.queries.GetLatestDeviceCommandByDeviceForUserQuery;
import com.claircore.device.application.queryservices.DeviceCommandQueryService;
import com.claircore.device.domain.repositories.DeviceAssignmentRepository;
import com.claircore.device.domain.repositories.DeviceCommandRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class DeviceCommandQueryServiceImpl implements DeviceCommandQueryService {

    private final DeviceCommandRepository deviceCommandRepository;
    private final DeviceAssignmentRepository deviceAssignmentRepository;

    public DeviceCommandQueryServiceImpl(
            DeviceCommandRepository deviceCommandRepository,
            DeviceAssignmentRepository deviceAssignmentRepository
    ) {
        this.deviceCommandRepository = deviceCommandRepository;
        this.deviceAssignmentRepository = deviceAssignmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeviceCommand> handle(GetDeviceCommandByIdForUserQuery query) {
        ensureDeviceBelongsToUser(query.deviceId(), query.userId());
        return deviceCommandRepository.findByDeviceIdAndCommandId(query.deviceId(), query.commandId());
    }

    @Override
    @Transactional
    public List<DeviceCommand> findClaimableForEdge(Instant leaseCutoff, int limit) {
        if (leaseCutoff == null) {
            throw new IllegalArgumentException("leaseCutoff must not be null");
        }
        // The repository requests a pessimistic lock while building the bounded candidate page;
        // keep this transaction read-write because PostgreSQL rejects SELECT FOR UPDATE in a
        // read-only transaction. The subsequent atomic claim still decides the winner.
        int bounded = Math.max(1, Math.min(limit, 500));
        return deviceCommandRepository.findPendingForEdge(null, leaseCutoff, bounded);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeviceCommand> handle(GetLatestDeviceCommandByDeviceForUserQuery query) {
        ensureDeviceBelongsToUser(query.deviceId(), query.userId());
        return deviceCommandRepository.findLatestByDeviceId(query.deviceId());
    }

    private void ensureDeviceBelongsToUser(java.util.UUID deviceId, com.claircore.device.domain.model.valueobjects.UserId userId) {
        var assignment = deviceAssignmentRepository
                .findByDeviceId(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device assignment not found"));

        if (assignment.getOwnerUserId() == null || !assignment.getOwnerUserId().equals(userId)) {
            throw new AccessDeniedException("Device does not belong to user");
        }
    }
}
