package com.claircore.device.application.internal.queryservices;

import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.queries.GetDeviceThresholdByMetricQuery;
import com.claircore.device.domain.model.queries.GetDeviceThresholdsByDeviceQuery;
import com.claircore.device.domain.model.valueobjects.DeviceMetricThresholdConfiguration;
import com.claircore.device.domain.model.valueobjects.MetricThreshold;
import com.claircore.device.application.queryservices.DeviceThresholdQueryService;
import com.claircore.device.domain.repositories.DeviceAssignmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class DeviceThresholdQueryServiceImpl implements DeviceThresholdQueryService {

    private final DeviceAssignmentRepository deviceAssignmentRepository;
    private final ObjectMapper objectMapper;

    public DeviceThresholdQueryServiceImpl(
            DeviceAssignmentRepository deviceAssignmentRepository,
            ObjectMapper objectMapper) {
        this.deviceAssignmentRepository = deviceAssignmentRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceMetricThresholdConfiguration> handle(GetDeviceThresholdsByDeviceQuery query) {
        DeviceAssignment assignment = deviceAssignmentRepository
                .findByDeviceId(query.deviceId())
                .orElseThrow(() -> new IllegalArgumentException("Device assignment not found"));

        if (assignment.getOwnerUserId() == null || !assignment.getOwnerUserId().equals(query.userId())) {
            throw new AccessDeniedException("Device does not belong to user");
        }

        return readThresholdsFromConfig(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeviceMetricThresholdConfiguration> handle(GetDeviceThresholdByMetricQuery query) {
        return deviceAssignmentRepository.findById(query.assignmentId())
                .flatMap(a -> a.findConfigurationValue(thresholdConfigKey(query.metric())))
                .flatMap(this::deserialize);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceMetricThresholdConfiguration> findAllByAssignmentId(UUID assignmentId) {
        return deviceAssignmentRepository.findById(assignmentId)
                .map(this::readThresholdsFromConfig)
                .orElseGet(List::of);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceMetricThresholdConfiguration> findEnabledByAssignmentId(UUID assignmentId) {
        return findAllByAssignmentId(assignmentId).stream()
                .filter(DeviceMetricThresholdConfiguration::enabled)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceMetricThresholdConfiguration> findEnabledByDeviceId(UUID deviceId) {
        return deviceAssignmentRepository.findByDeviceId(deviceId)
                .map(this::readThresholdsFromConfig).orElseGet(List::of).stream()
                .filter(DeviceMetricThresholdConfiguration::enabled).toList();
    }

    private List<DeviceMetricThresholdConfiguration> readThresholdsFromConfig(DeviceAssignment assignment) {
        return Stream.of(MetricThreshold.values())
                .map(metric -> assignment.findConfigurationValue(thresholdConfigKey(metric))
                        .flatMap(this::deserialize)
                        .orElse(null))
                .filter(t -> t != null)
                .toList();
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
