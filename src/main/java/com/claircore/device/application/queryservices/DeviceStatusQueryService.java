package com.claircore.device.application.queryservices;

import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.queries.GetDeviceStatusByDeviceIdForUserQuery;

import java.util.Optional;

public interface DeviceStatusQueryService {
    Optional<DeviceAssignment> handle(GetDeviceStatusByDeviceIdForUserQuery query);
}

