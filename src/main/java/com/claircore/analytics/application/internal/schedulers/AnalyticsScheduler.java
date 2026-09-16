package com.claircore.analytics.application.internal.schedulers;

import com.claircore.analytics.application.commandservices.DailySummaryCommandService;
import com.claircore.analytics.application.commandservices.MonthlySummaryCommandService;
import com.claircore.analytics.application.commandservices.SnapshotAggregationCommandService;
import com.claircore.analytics.domain.model.commands.AggregateHourlySnapshotCommand;
import com.claircore.analytics.domain.model.commands.GenerateDailySummaryCommand;
import com.claircore.analytics.domain.model.commands.GenerateMonthlySummaryCommand;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * The only place a clock decides that aggregation should run. It holds no logic of its own: each
 * method picks the window and issues the command.
 *
 * <p>Keeping the schedule out of the command services also fixes a defect. The aggregation used to
 * be scheduled on the same bean that carried {@code @Transactional}, and a {@code @Scheduled} method
 * calling its own transactional method calls it on {@code this}, not through the proxy — so the
 * nightly and monthly runs executed with no transaction at all. Crossing a bean boundary here means
 * the proxy is in the path and the annotation takes effect.
 */
@Component
public class AnalyticsScheduler {

    private final DailySummaryCommandService dailySummaryCommandService;
    private final MonthlySummaryCommandService monthlySummaryCommandService;
    private final SnapshotAggregationCommandService snapshotAggregationCommandService;
    private final ZoneId reportZone;

    public AnalyticsScheduler(
            DailySummaryCommandService dailySummaryCommandService,
            MonthlySummaryCommandService monthlySummaryCommandService,
            SnapshotAggregationCommandService snapshotAggregationCommandService,
            @Value("${claircore.reports.zone:America/Lima}") String reportZone
    ) {
        this.dailySummaryCommandService = dailySummaryCommandService;
        this.monthlySummaryCommandService = monthlySummaryCommandService;
        this.snapshotAggregationCommandService = snapshotAggregationCommandService;
        this.reportZone = ZoneId.of(reportZone);
    }

    /** Fires on the hour and snapshots the hour that just closed. */
    @Scheduled(cron = "0 0 * * * *")
    public void aggregateHourlySnapshots() {
        snapshotAggregationCommandService.handle(
                new AggregateHourlySnapshotCommand(Instant.now().truncatedTo(ChronoUnit.HOURS)));
    }

    /** Fires at 00:15 local time and summarises the day that just closed. */
    @Scheduled(cron = "0 15 0 * * *", zone = "${claircore.reports.zone:America/Lima}")
    public void aggregatePreviousDay() {
        dailySummaryCommandService.handle(
                new GenerateDailySummaryCommand(LocalDate.now(reportZone).minusDays(1)));
    }

    /** Fires at 01:00 local time on the 1st and cascades the month that just closed. */
    @Scheduled(cron = "0 0 1 1 * *", zone = "${claircore.reports.zone:America/Lima}")
    public void aggregatePreviousMonth() {
        monthlySummaryCommandService.handle(
                new GenerateMonthlySummaryCommand(YearMonth.now(reportZone).minusMonths(1)));
    }
}
