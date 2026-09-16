package com.claircore.analytics.application.internal.eventhandlers;

import com.claircore.analytics.application.internal.outboundservices.acl.ExternalEvaluationService;
import com.claircore.analytics.application.internal.outboundservices.cache.LiveMetricsStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * The live window lives in memory and is empty after a restart, which read as "device offline"
 * on the dashboard until the next reading. Refill it from the readings of the last five minutes.
 */
@Component
public class LiveMetricsWarmUpOnStartup implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LiveMetricsWarmUpOnStartup.class);
    static final Duration WINDOW = Duration.ofMinutes(5);

    private final ExternalEvaluationService evaluations;
    private final LiveMetricsStore liveMetricsStore;

    public LiveMetricsWarmUpOnStartup(ExternalEvaluationService evaluations, LiveMetricsStore liveMetricsStore) {
        this.evaluations = evaluations;
        this.liveMetricsStore = liveMetricsStore;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        warmUp(Instant.now());
    }

    int warmUp(Instant now) {
        try {
            var readings = evaluations.fetchReadings(now.minus(WINDOW), now);
            for (var reading : readings) {
                liveMetricsStore.getOrCreate(reading.deviceId())
                        .add(reading.recordedAt(), reading.co2(), reading.pm2_5(), reading.temperature(), reading.humidity());
            }
            if (!readings.isEmpty()) {
                LOGGER.info("Live metrics warmed up with {} reading(s) from the last {} minutes", readings.size(), WINDOW.toMinutes());
            }
            return readings.size();
        } catch (Exception e) {
            LOGGER.warn("Live metrics warm-up skipped: {}", e.getMessage());
            return 0;
        }
    }
}
