package com.claircore.analytics.application.internal.commandservices;

import com.claircore.analytics.application.commandservices.MonthlySummaryCommandService;
import com.claircore.analytics.domain.model.aggregates.DeviceDailySummary;
import com.claircore.analytics.domain.model.aggregates.DeviceMonthlySummary;
import com.claircore.analytics.domain.model.commands.GenerateMonthlySummaryCommand;
import com.claircore.analytics.domain.model.valueobjects.AqiCategoryBreakdown;
import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.domain.model.valueobjects.MetricStats;
import com.claircore.analytics.domain.repositories.DeviceDailySummaryRepository;
import com.claircore.analytics.domain.repositories.DeviceMonthlySummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds {@link DeviceMonthlySummary} rows by cascading a month's {@link DeviceDailySummary} rows.
 * Averages are reading-count weighted, extremes are extremes-of-extremes, and category breakdowns
 * are summed — so the monthly figures are exact, never an average-of-averages. Issuing the command
 * for an earlier month backfills it.
 */
@Service
public class MonthlySummaryCommandServiceImpl implements MonthlySummaryCommandService {

    private static final Logger logger = LoggerFactory.getLogger(MonthlySummaryCommandServiceImpl.class);

    private final DeviceDailySummaryRepository dailySummaryRepository;
    private final DeviceMonthlySummaryRepository monthlySummaryRepository;

    public MonthlySummaryCommandServiceImpl(
            DeviceDailySummaryRepository dailySummaryRepository,
            DeviceMonthlySummaryRepository monthlySummaryRepository
    ) {
        this.dailySummaryRepository = dailySummaryRepository;
        this.monthlySummaryRepository = monthlySummaryRepository;
    }

    /** Recomputes existing rows from the latest daily summaries. */
    @Override
    @Transactional
    public int handle(GenerateMonthlySummaryCommand command) {
        LocalDate firstDay = command.month().atDay(1);
        LocalDate lastDay = command.month().atEndOfMonth();

        List<DeviceDailySummary> dailies = dailySummaryRepository.findAllByDateBetween(firstDay, lastDay);

        Map<UUID, MonthlyAccumulator> byDevice = new LinkedHashMap<>();
        for (DeviceDailySummary daily : dailies) {
            byDevice.computeIfAbsent(daily.getDeviceId().value(), id -> new MonthlyAccumulator()).add(daily);
        }

        int written = 0;
        for (Map.Entry<UUID, MonthlyAccumulator> entry : byDevice.entrySet()) {
            UUID deviceId = entry.getKey();
            DeviceMonthlySummary summary = entry.getValue().toSummary(new DeviceId(deviceId), firstDay,
                    previousMonthAqi(deviceId, firstDay));
            monthlySummaryRepository.save(summary);
            monthlySummaryRepository.findByDeviceIdAndMonth(deviceId, firstDay.plusMonths(1))
                    .ifPresent(next -> monthlySummaryRepository.save(next.withPreviousAqi(summary.getAverageAqi())));
            written++;
        }
        logger.info("Monthly report aggregation for {} produced {} summaries", firstDay, written);
        return written;
    }

    private Integer previousMonthAqi(UUID deviceId, LocalDate month) {
        return monthlySummaryRepository.findByDeviceIdAndMonth(deviceId, month.minusMonths(1))
                .map(DeviceMonthlySummary::getAverageAqi)
                .orElse(null);
    }

    /** Reading-count-weighted cascade over one device's daily summaries for a month. */
    private static final class MonthlyAccumulator {
        private double co2WeightedAvg, pm25WeightedAvg, tempWeightedAvg, humWeightedAvg;
        private double co2Min = Double.MAX_VALUE, co2Max = -Double.MAX_VALUE;
        private double pm25Min = Double.MAX_VALUE, pm25Max = -Double.MAX_VALUE;
        private double tempMin = Double.MAX_VALUE, tempMax = -Double.MAX_VALUE;
        private double humMin = Double.MAX_VALUE, humMax = -Double.MAX_VALUE;
        private double peakPm25 = -Double.MAX_VALUE;
        private Instant peakPm25At;
        private long readingCount;
        private int daysCovered;
        private AqiCategoryBreakdown breakdown = AqiCategoryBreakdown.empty();

        void add(DeviceDailySummary d) {
            long w = d.getReadingCount();
            co2WeightedAvg += d.getCo2().avg() * w;
            pm25WeightedAvg += d.getPm2_5().avg() * w;
            tempWeightedAvg += d.getTemperature().avg() * w;
            humWeightedAvg += d.getHumidity().avg() * w;
            co2Min = Math.min(co2Min, d.getCo2().min()); co2Max = Math.max(co2Max, d.getCo2().max());
            pm25Min = Math.min(pm25Min, d.getPm2_5().min()); pm25Max = Math.max(pm25Max, d.getPm2_5().max());
            tempMin = Math.min(tempMin, d.getTemperature().min()); tempMax = Math.max(tempMax, d.getTemperature().max());
            humMin = Math.min(humMin, d.getHumidity().min()); humMax = Math.max(humMax, d.getHumidity().max());
            if (d.getPeakPm2_5() > peakPm25) { peakPm25 = d.getPeakPm2_5(); peakPm25At = d.getPeakPm2_5At(); }
            breakdown = breakdown.plus(d.getCategoryBreakdown());
            readingCount += w;
            daysCovered++;
        }

        DeviceMonthlySummary toSummary(DeviceId deviceId, LocalDate month, Integer previousMonthAqi) {
            int averageAqi = new com.claircore.analytics.domain.services.AqiCalculator()
                    .calculateAqi(pm25WeightedAvg / readingCount).value();
            Double deltaPct = (previousMonthAqi != null && previousMonthAqi > 0)
                    ? ((averageAqi - previousMonthAqi) * 100.0) / previousMonthAqi
                    : null;
            return new DeviceMonthlySummary(
                    deviceId,
                    month,
                    MetricStats.of(co2WeightedAvg / readingCount, co2Min, co2Max),
                    MetricStats.of(pm25WeightedAvg / readingCount, pm25Min, pm25Max),
                    MetricStats.of(tempWeightedAvg / readingCount, tempMin, tempMax),
                    MetricStats.of(humWeightedAvg / readingCount, humMin, humMax),
                    peakPm25,
                    peakPm25At,
                    averageAqi,
                    breakdown.dominant(),
                    breakdown,
                    readingCount,
                    daysCovered,
                    deltaPct
            );
        }
    }
}
