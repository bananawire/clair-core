package com.claircore.device.application.commandservices;

import com.claircore.device.domain.model.commands.RemoveDeviceThresholdCommand;
import com.claircore.device.domain.model.commands.WriteDeviceThresholdCommand;
import com.claircore.device.domain.model.valueobjects.DeviceMetricThresholdConfiguration;

import java.util.Optional;
import java.util.UUID;

public interface DeviceThresholdCommandService {
    DeviceMetricThresholdConfiguration handle(WriteDeviceThresholdCommand command);
    void handle(RemoveDeviceThresholdCommand command);
    Optional<DeviceMetricThresholdConfiguration> findByDeviceAndMetric(UUID deviceId, com.claircore.device.domain.model.valueobjects.MetricThreshold metric);
}
