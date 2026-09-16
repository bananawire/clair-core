package com.claircore.analytics.domain.model.valueobjects;

/**
 * The four metric averages over a window of snapshots. A read model, produced by the repository,
 * never stored; absent entirely when the window holds no snapshots.
 */
public record MetricAverages(
        double co2,
        double pm2_5,
        double temperature,
        double humidity
) {
}
