package com.claircore.analytics.infrastructure.persistence.jpa.repositories;

/**
 * Storage shape of the averages query. Every component is null when the window holds no snapshot —
 * an aggregate over no rows still returns one row — which is how the adapter tells "no data" from
 * "averaged to zero".
 */
public record MetricAveragesProjection(Double co2, Double pm2_5, Double temperature, Double humidity) {

    public boolean isEmpty() {
        return co2 == null || pm2_5 == null || temperature == null || humidity == null;
    }
}
