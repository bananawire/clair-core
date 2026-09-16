package com.claircore.alerting.interfaces.acl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertingContextFacade {
    List<AlertDetails> getActiveAlertsByDeviceId(UUID deviceId);
    Optional<AlertDetails> getAlertDetailsById(UUID alertId);

    /**
     * Ownership-scoped alert summaries across all owned devices.
     *
     * <p>Statuses are names, not alerting's {@code AlertStatus} enum: a consumer that names another
     * context's enum is coupled to its storage mapping. An unrecognised name matches nothing.
     */
    List<AlertDetails> getRecentAlertsByOwnerId(UUID ownerUserId, List<String> statuses, int limit);
}
