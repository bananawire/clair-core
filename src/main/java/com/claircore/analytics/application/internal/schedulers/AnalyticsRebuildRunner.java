
package com.claircore.analytics.application.internal.schedulers;

import com.claircore.analytics.application.commandservices.DailySummaryCommandService;
import com.claircore.analytics.application.commandservices.SnapshotAggregationCommandService;
import com.claircore.analytics.domain.model.commands.AggregateHourlySnapshotCommand;
import com.claircore.analytics.domain.model.commands.GenerateDailySummaryCommand;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/** Explicit operator-triggered rebuild after a formula change or late telemetry arrival. */
@Component
@ConditionalOnProperty(name = "claircore.reports.rebuild-from")
public class AnalyticsRebuildRunner implements ApplicationRunner {
    private final DailySummaryCommandService dailies;
    private final SnapshotAggregationCommandService snapshots;
    private final LocalDate from;
    private final LocalDate through;
    private final ZoneId zone;

    public AnalyticsRebuildRunner(DailySummaryCommandService dailies, SnapshotAggregationCommandService snapshots,
            @Value("${claircore.reports.rebuild-from}") LocalDate from,
            @Value("${claircore.reports.rebuild-through}") LocalDate through,
            @Value("${claircore.reports.zone:America/Lima}") String zone) {
        if (through.isBefore(from)) throw new IllegalArgumentException("rebuild-through must be on or after rebuild-from");
        this.dailies = dailies; this.snapshots = snapshots;
        this.from = from; this.through = through; this.zone = ZoneId.of(zone);
    }

    @Override public void run(ApplicationArguments args) {
        if (!through.isBefore(LocalDate.now(zone)))
            throw new IllegalArgumentException("Rebuild only closed days; rebuild-through must be before today");
        var start = from.atStartOfDay(zone).toInstant().truncatedTo(ChronoUnit.HOURS);
        var end = through.plusDays(1).atStartOfDay(zone).toInstant();
        for (var hourEnd = start.plus(1, ChronoUnit.HOURS); !hourEnd.isAfter(end); hourEnd = hourEnd.plus(1, ChronoUnit.HOURS)) {
            snapshots.handle(new AggregateHourlySnapshotCommand(hourEnd));
        }
        for (var day = from; !day.isAfter(through); day = day.plusDays(1)) {
            dailies.handle(new GenerateDailySummaryCommand(day));
        }
    }
}
