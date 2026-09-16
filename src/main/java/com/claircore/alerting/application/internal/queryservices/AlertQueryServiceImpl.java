package com.claircore.alerting.application.internal.queryservices;

import com.claircore.alerting.application.internal.outboundservices.acl.ExternalAlertingDeviceService;
import com.claircore.alerting.application.queryservices.AlertQueryService;
import com.claircore.alerting.domain.model.aggregates.Alert;
import com.claircore.alerting.domain.model.queries.GetAlertsByDeviceQuery;
import com.claircore.alerting.domain.model.queries.GetAlertsByOwnerQuery;
import com.claircore.alerting.domain.model.queries.GetAlertsBySpaceQuery;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.domain.model.valueobjects.DailyAlertCount;
import com.claircore.alerting.domain.repositories.AlertRepository;
import com.claircore.shared.domain.model.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class AlertQueryServiceImpl implements AlertQueryService {

    private final AlertRepository alertRepository;
    private final ExternalAlertingDeviceService externalDeviceService;

    public AlertQueryServiceImpl(AlertRepository alertRepository,
                                 ExternalAlertingDeviceService externalDeviceService) {
        this.alertRepository = alertRepository;
        this.externalDeviceService = externalDeviceService;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<Alert> findById(UUID alertId) {
        return alertRepository.findById(alertId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Alert> findActiveByDeviceId(UUID deviceId) {
        return alertRepository.findByDeviceIdAndStatus(deviceId, AlertStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Alert> findRecentByOwnerId(UUID ownerId, List<AlertStatus> statuses, int limit) {
        if (ownerId == null || limit <= 0) return List.of();
        var deviceIds = externalDeviceService.fetchDeviceIdsByOwnerId(ownerId);
        if (deviceIds == null || deviceIds.isEmpty()) return List.of();
        return (statuses.isEmpty()
                ? alertRepository.findByDeviceIdIn(deviceIds, 0, limit)
                : alertRepository.findByDeviceIdInAndStatusIn(deviceIds, statuses, 0, limit)).items();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Alert> fetchByDevice(GetAlertsByDeviceQuery query) {
        return alertRepository.findByDeviceId(query.deviceId(), query.page(), query.size());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Alert> fetchBySpace(GetAlertsBySpaceQuery query) {
        return alertRepository.findBySpaceId(query.spaceId(), query.page(), query.size());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Alert> fetchByOwner(GetAlertsByOwnerQuery query, List<UUID> ownerDeviceIds) {
        if (ownerDeviceIds == null || ownerDeviceIds.isEmpty()) {
            return PageResult.empty(query.page(), query.size());
        }
        return alertRepository.findByDeviceIdIn(ownerDeviceIds, query.page(), query.size());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Alert> fetchByDeviceAndStatus(GetAlertsByDeviceQuery query, List<AlertStatus> statuses) {
        return alertRepository.findByDeviceIdAndStatusIn(query.deviceId(), statuses, query.page(), query.size());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Alert> fetchBySpaceAndStatus(GetAlertsBySpaceQuery query, List<AlertStatus> statuses) {
        return alertRepository.findBySpaceIdAndStatusIn(query.spaceId(), statuses, query.page(), query.size());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Alert> fetchByOwnerAndStatus(GetAlertsByOwnerQuery query, List<UUID> ownerDeviceIds, List<AlertStatus> statuses) {
        if (ownerDeviceIds == null || ownerDeviceIds.isEmpty()) {
            return PageResult.empty(query.page(), query.size());
        }
        if (statuses == null || statuses.isEmpty()) {
            return fetchByOwner(query, ownerDeviceIds);
        }
        return alertRepository.findByDeviceIdInAndStatusIn(ownerDeviceIds, statuses, query.page(), query.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyAlertCount> fetchDailySummaryBySpace(UUID spaceId, int days) {
        return alertRepository.countAlertsPerDayBySpaceId(spaceId, since(days));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyAlertCount> fetchDailySummaryByOwner(UUID ownerUserId, List<UUID> ownerDeviceIds, int days) {
        if (ownerDeviceIds == null || ownerDeviceIds.isEmpty()) {
            return List.of();
        }
        return alertRepository.countAlertsPerDayByDeviceIds(ownerDeviceIds, since(days));
    }

    private static Instant since(int days) {
        return LocalDate.now(ZoneOffset.UTC).minusDays(days).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
