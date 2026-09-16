package com.claircore.alerting.application.queryservices;

import com.claircore.alerting.domain.model.aggregates.Alert;
import com.claircore.alerting.domain.model.queries.GetAlertsByDeviceQuery;
import com.claircore.alerting.domain.model.queries.GetAlertsByOwnerQuery;
import com.claircore.alerting.domain.model.queries.GetAlertsBySpaceQuery;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.domain.model.valueobjects.DailyAlertCount;
import com.claircore.shared.domain.model.PageResult;

import java.util.List;
import java.util.UUID;

/** Inbound port for the alerting read side. */
public interface AlertQueryService {
    java.util.Optional<Alert> findById(UUID alertId);
    List<Alert> findActiveByDeviceId(UUID deviceId);
    List<Alert> findRecentByOwnerId(UUID ownerId, List<AlertStatus> statuses, int limit);
    PageResult<Alert> fetchByDevice(GetAlertsByDeviceQuery query);
    PageResult<Alert> fetchBySpace(GetAlertsBySpaceQuery query);
    PageResult<Alert> fetchByOwner(GetAlertsByOwnerQuery query, List<UUID> ownerDeviceIds);
    PageResult<Alert> fetchByDeviceAndStatus(GetAlertsByDeviceQuery query, List<AlertStatus> statuses);
    PageResult<Alert> fetchBySpaceAndStatus(GetAlertsBySpaceQuery query, List<AlertStatus> statuses);
    PageResult<Alert> fetchByOwnerAndStatus(GetAlertsByOwnerQuery query, List<UUID> ownerDeviceIds, List<AlertStatus> statuses);
    List<DailyAlertCount> fetchDailySummaryBySpace(UUID spaceId, int days);
    List<DailyAlertCount> fetchDailySummaryByOwner(UUID ownerUserId, List<UUID> ownerDeviceIds, int days);
}
