package com.claircore.alerting.application.acl;

import com.claircore.alerting.domain.model.aggregates.Alert;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.interfaces.acl.AlertDetails;
import com.claircore.alerting.interfaces.acl.AlertingContextFacade;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AlertingContextFacadeImpl implements AlertingContextFacade {

    private final com.claircore.alerting.application.queryservices.AlertQueryService queryService;

    public AlertingContextFacadeImpl(com.claircore.alerting.application.queryservices.AlertQueryService queryService) {
        this.queryService = queryService;
    }

    @Override
    public List<AlertDetails> getActiveAlertsByDeviceId(UUID deviceId) {
        return queryService.findActiveByDeviceId(deviceId).stream()
                .map(AlertingContextFacadeImpl::toDto)
                .toList();
    }

    @Override
    public Optional<AlertDetails> getAlertDetailsById(UUID alertId) {
        return queryService.findById(alertId).map(AlertingContextFacadeImpl::toDto);
    }

    @Override
    public List<AlertDetails> getRecentAlertsByOwnerId(UUID ownerUserId, List<String> statuses, int limit) {
        return queryService.findRecentByOwnerId(ownerUserId, parseStatuses(statuses), limit)
                .stream().map(AlertingContextFacadeImpl::toDto).toList();
    }

    /** An unrecognised name is dropped rather than throwing: the caller is another context. */
    private static List<AlertStatus> parseStatuses(List<String> statuses) {
        if (statuses == null) return List.of();
        return statuses.stream()
                .map(AlertingContextFacadeImpl::parseStatus)
                .flatMap(Optional::stream)
                .toList();
    }

    private static Optional<AlertStatus> parseStatus(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        try {
            return Optional.of(AlertStatus.valueOf(name));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private static AlertDetails toDto(Alert alert) {
        return new AlertDetails(
                alert.getId(),
                alert.getDeviceId(),
                alert.getSpaceId(),
                alert.getDeviceName(),
                alert.getMetric().name(),
                alert.getThresholdValue(),
                alert.getActualValue(),
                alert.getMessage(),
                alert.getStatus().name(),
                alert.getSeverity().name(),
                alert.getOccurredAt()
        );
    }
}
