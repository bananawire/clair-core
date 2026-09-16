package com.claircore.device.application.internal.queryservices;

import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.queries.GetDeviceStatusByDeviceIdForUserQuery;
import com.claircore.device.application.queryservices.DeviceStatusQueryService;
import com.claircore.device.domain.repositories.DeviceAssignmentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class DeviceStatusQueryServiceImpl implements DeviceStatusQueryService {

    private final DeviceAssignmentRepository deviceAssignmentRepository;

    public DeviceStatusQueryServiceImpl(DeviceAssignmentRepository deviceAssignmentRepository) {
        this.deviceAssignmentRepository = deviceAssignmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeviceAssignment> handle(GetDeviceStatusByDeviceIdForUserQuery query) {
        var assignment = deviceAssignmentRepository.findByDeviceId(query.deviceId());
        if (assignment.isEmpty()) {
            return Optional.empty();
        }

        var resolved = assignment.get();
        if (resolved.getOwnerUserId() == null || !resolved.getOwnerUserId().equals(query.userId())) {
            throw new AccessDeniedException("Device does not belong to user");
        }

        return assignment;
    }
}

