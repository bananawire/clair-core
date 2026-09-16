package com.claircore.device.domain.model.queries;

/**
 * Page of devices the LocalEdge bounded context plans to service in one cycle. Tombstones
 * (deleted=true) are excluded unless the caller explicitly opts in.
 */
public record GetDevicesForTelemetryQuery(int limit, boolean includeDeleted) {
    public GetDevicesForTelemetryQuery {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        if (limit > 500) {
            throw new IllegalArgumentException("limit must be at most 500");
        }
    }
}
