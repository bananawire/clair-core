package com.claircore.device.application.queryservices;

import com.claircore.device.domain.model.queries.GetDeviceThresholdByMetricQuery;
import com.claircore.device.domain.model.queries.GetDeviceThresholdsByDeviceQuery;
import com.claircore.device.domain.model.valueobjects.DeviceMetricThresholdConfiguration;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceThresholdQueryService {
    List<DeviceMetricThresholdConfiguration> findEnabledByDeviceId(UUID deviceId);
    List<DeviceMetricThresholdConfiguration> handle(GetDeviceThresholdsByDeviceQuery query);
    Optional<DeviceMetricThresholdConfiguration> handle(GetDeviceThresholdByMetricQuery query);
    List<DeviceMetricThresholdConfiguration> findAllByAssignmentId(UUID assignmentId);
    List<DeviceMetricThresholdConfiguration> findEnabledByAssignmentId(UUID assignmentId);
}
