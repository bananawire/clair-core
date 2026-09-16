package com.claircore.analytics.infrastructure.cache;

import com.claircore.analytics.application.internal.outboundservices.cache.KpiLiveMetricsBuffer;
import com.claircore.analytics.application.internal.outboundservices.cache.LiveMetricsStore;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * In-process {@link LiveMetricsStore}. A device that stops reporting drops out ten minutes after it
 * was last read, so an idle fleet costs nothing.
 */
@Component
public class KpiLiveMetricsCache implements LiveMetricsStore {

    private final Cache<UUID, KpiLiveMetricsBuffer> cache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();

    @Override
    public KpiLiveMetricsBuffer getOrCreate(UUID deviceId) {
        return cache.get(deviceId, k -> new KpiLiveMetricsBuffer());
    }

    @Override
    public KpiLiveMetricsBuffer getIfPresent(UUID deviceId) {
        return cache.getIfPresent(deviceId);
    }
}
