package com.claircore.analytics.application.internal.outboundservices.cache;

import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.util.Comparator;
import java.util.Optional;
import java.util.PriorityQueue;

/** Single-instance five-minute window, ordered by measurement time (including late arrivals). */
public class KpiLiveMetricsBuffer {
    private static final Duration WINDOW = Duration.ofMinutes(5);
    private final PriorityQueue<Reading> readings = new PriorityQueue<>(Comparator.comparing(Reading::timestamp));
    private final Clock clock;

    public KpiLiveMetricsBuffer() { this(Clock.systemUTC()); }
    public KpiLiveMetricsBuffer(Clock clock) { this.clock = clock; }

    public synchronized void add(Instant timestamp, double co2, double pm2_5, double temperature, double humidity) {
        Instant now = clock.instant();
        prune(now);
        if (timestamp.isBefore(now.minus(WINDOW)) || timestamp.isAfter(now)) return;
        readings.add(new Reading(timestamp, co2, pm2_5, temperature, humidity));
    }

    public synchronized boolean isEmpty() {
        prune(clock.instant());
        return readings.isEmpty();
    }

    private void prune(Instant now) {
        Instant cutoff = now.minus(WINDOW);
        while (!readings.isEmpty() && readings.peek().timestamp().isBefore(cutoff)) readings.poll();
    }

    /** Count, sums and last measurement come from the same locked window. */
    public synchronized Optional<Averages> computeAverages() {
        prune(clock.instant());
        if (readings.isEmpty()) return Optional.empty();
        double co2 = 0, pm = 0, temp = 0, humidity = 0;
        Instant latest = Instant.MIN;
        for (Reading r : readings) {
            co2 += r.co2(); pm += r.pm2_5(); temp += r.temperature(); humidity += r.humidity();
            if (r.timestamp().isAfter(latest)) latest = r.timestamp();
        }
        int count = readings.size();
        return Optional.of(new Averages(co2 / count, pm / count, temp / count, humidity / count, count, latest));
    }

    public record Reading(Instant timestamp, double co2, double pm2_5, double temperature, double humidity) {}
    public record Averages(double co2, double pm2_5, double temperature, double humidity,
                           int readingCount, Instant measuredAt) {}
}
