package com.claircore.device.application.internal.commandservices;

import com.claircore.device.domain.model.commands.RemoveDeviceThresholdCommand;
import com.claircore.device.domain.model.commands.WriteDeviceThresholdCommand;
import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.valueobjects.DeviceMetricThresholdConfiguration;
import com.claircore.device.domain.model.valueobjects.MetricThreshold;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.application.commandservices.DeviceThresholdCommandService;
import com.claircore.device.domain.repositories.DeviceAssignmentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class DeviceThresholdCommandServiceImpl implements DeviceThresholdCommandService {

    private final DeviceAssignmentRepository deviceAssignmentRepository;
    private final ObjectMapper objectMapper;

    public DeviceThresholdCommandServiceImpl(
            DeviceAssignmentRepository deviceAssignmentRepository,
            ObjectMapper objectMapper) {
        this.deviceAssignmentRepository = deviceAssignmentRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public DeviceMetricThresholdConfiguration handle(WriteDeviceThresholdCommand command) {
        DeviceAssignment assignment = loadOwnedAssignment(command.deviceId(), command.userId());

        String configKey = thresholdConfigKey(command.metric());
        boolean exists = assignment.findConfigurationValue(configKey).isPresent();

        switch (command.intent()) {
            case CREATE -> {
                if (exists) throw new IllegalArgumentException("Threshold already exists for the specified metric");
            }
            case UPDATE -> {
                if (!exists) throw new IllegalArgumentException("Threshold not found for the specified metric");
            }
        }

        var configuration = new DeviceMetricThresholdConfiguration(
                command.metric(),
                command.value(),
                command.enabled()
        );

        try {
            String json = objectMapper.writeValueAsString(configuration);
            assignment.putConfigurationValue(configKey, json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize threshold configuration", e);
        }

        deviceAssignmentRepository.save(assignment);
        return configuration;
    }

    @Override
    @Transactional
    public void handle(RemoveDeviceThresholdCommand command) {
        DeviceAssignment assignment = loadOwnedAssignment(command.deviceId(), command.userId());

        String configKey = thresholdConfigKey(command.metric());
        boolean exists = assignment.findConfigurationValue(configKey).isPresent();
        if (!exists) throw new IllegalArgumentException("Threshold not found for the specified metric");

        assignment.removeConfigurationValue(configKey);
        deviceAssignmentRepository.save(assignment);
    }

    @Override
    public Optional<DeviceMetricThresholdConfiguration> findByDeviceAndMetric(UUID deviceId, MetricThreshold metric) {
        return deviceAssignmentRepository.findByDeviceId(deviceId)
                .flatMap(a -> a.findConfigurationValue(thresholdConfigKey(metric)))
                .flatMap(json -> deserialize(json));
    }

    private DeviceAssignment loadOwnedAssignment(UUID deviceId, UserId userId) {
        DeviceAssignment assignment = deviceAssignmentRepository
                .findByDeviceIdForUpdate(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device assignment not found"));

        if (assignment.getOwnerUserId() == null || !assignment.getOwnerUserId().equals(userId)) {
            throw new AccessDeniedException("Device does not belong to user");
        }

        return assignment;
    }

    private Optional<DeviceMetricThresholdConfiguration> deserialize(String json) {
        try {
            return Optional.of(objectMapper.readValue(json, DeviceMetricThresholdConfiguration.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    static String thresholdConfigKey(MetricThreshold metric) {
        return "threshold." + metric.name();
    }
}
