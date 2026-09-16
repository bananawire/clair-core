package com.claircore.analytics.application.internal.outboundservices.cache;

import java.util.UUID;

/**
 * Outbound port for the short-lived buffer of readings behind the LIVE window. Not a repository:
 * nothing here survives a restart, and nothing here is a source of truth — the stored snapshots are.
 */
public interface LiveMetricsStore {

    /** The device's buffer, created empty if this is its first reading. */
    KpiLiveMetricsBuffer getOrCreate(UUID deviceId);

    /** The device's buffer, or null when it has none — no reading in the retention window. */
    KpiLiveMetricsBuffer getIfPresent(UUID deviceId);
}
