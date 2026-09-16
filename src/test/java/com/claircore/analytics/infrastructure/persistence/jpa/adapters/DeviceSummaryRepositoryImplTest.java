package com.claircore.analytics.infrastructure.persistence.jpa.adapters;

import com.claircore.analytics.domain.model.aggregates.DeviceDailySummary;
import com.claircore.analytics.domain.model.aggregates.DeviceMonthlySummary;
import com.claircore.analytics.domain.model.valueobjects.AqiCategory;
import com.claircore.analytics.domain.model.valueobjects.AqiCategoryBreakdown;
import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.domain.model.valueobjects.MetricStats;
import com.claircore.analytics.domain.repositories.DeviceDailySummaryRepository;
import com.claircore.analytics.domain.repositories.DeviceMonthlySummaryRepository;
import com.claircore.shared.infrastructure.config.JpaAuditingConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JpaAuditingConfiguration.class,
        DeviceDailySummaryRepositoryImpl.class,
        DeviceMonthlySummaryRepositoryImpl.class})
class DeviceSummaryRepositoryImplTest {

    private static final LocalDate DAY = LocalDate.of(2026, 5, 16);
    private static final Instant PEAK_AT = Instant.parse("2026-05-16T14:30:00Z");

    @Autowired
    private DeviceDailySummaryRepository dailyRepository;

    @Autowired
    private DeviceMonthlySummaryRepository monthlyRepository;

    @Test
    void roundTripsEveryDailyFieldThroughStorage() {
        UUID deviceId = UUID.randomUUID();
        var saved = dailyRepository.save(daily(deviceId, DAY, 100L));

        var found = dailyRepository.findByDeviceIdAndDate(deviceId, DAY).orElseThrow();

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getDeviceId()).isEqualTo(new DeviceId(deviceId));
        assertThat(found.getSummaryDate()).isEqualTo(DAY);
        assertThat(found.getCo2()).isEqualTo(MetricStats.of(450.0, 400.0, 500.0));
        assertThat(found.getPm2_5()).isEqualTo(MetricStats.of(12.0, 8.0, 40.0));
        assertThat(found.getTemperature()).isEqualTo(MetricStats.of(23.5, 20.0, 27.0));
        assertThat(found.getHumidity()).isEqualTo(MetricStats.of(52.0, 45.0, 60.0));
        assertThat(found.getPeakPm2_5()).isEqualTo(40.0);
        assertThat(found.getPeakPm2_5At()).isEqualTo(PEAK_AT);
        assertThat(found.getAverageAqi()).isEqualTo(55);
        assertThat(found.getDominantAqiCategory()).isEqualTo(AqiCategory.MODERATE);
        assertThat(found.getCategoryBreakdown()).isEqualTo(new AqiCategoryBreakdown(40, 55, 3, 2, 0, 0));
        assertThat(found.getReadingCount()).isEqualTo(100L);
        assertThat(found.getAqiDeltaPct()).isEqualTo(-3.5);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void latestDailyIsTheNewestSummaryDate() {
        UUID deviceId = UUID.randomUUID();
        dailyRepository.save(daily(deviceId, DAY, 100L));
        dailyRepository.save(daily(deviceId, DAY.plusDays(1), 200L));

        var latest = dailyRepository.findLatestByDeviceId(deviceId).orElseThrow();

        assertThat(latest.getSummaryDate()).isEqualTo(DAY.plusDays(1));
    }

    @Test
    void existsByDeviceAndDateIsWhatMakesTheAggregationIdempotent() {
        UUID deviceId = UUID.randomUUID();
        dailyRepository.save(daily(deviceId, DAY, 100L));

        assertThat(dailyRepository.existsByDeviceIdAndDate(deviceId, DAY)).isTrue();
        assertThat(dailyRepository.existsByDeviceIdAndDate(deviceId, DAY.plusDays(1))).isFalse();
    }

    @Test
    void allByDateBetweenIsInclusiveOfBothEnds() {
        UUID deviceId = UUID.randomUUID();
        dailyRepository.save(daily(deviceId, DAY, 100L));
        dailyRepository.save(daily(deviceId, DAY.plusDays(1), 200L));
        dailyRepository.save(daily(deviceId, DAY.plusDays(2), 300L));

        var found = dailyRepository.findAllByDateBetween(DAY, DAY.plusDays(1));

        assertThat(found).extracting(DeviceDailySummary::getSummaryDate)
                .containsExactly(DAY, DAY.plusDays(1));
    }

    @Test
    void roundTripsAMonthlySummaryAndNormalisesTheMonthToItsFirstDay() {
        UUID deviceId = UUID.randomUUID();
        monthlyRepository.save(monthly(deviceId, DAY));

        // Looked up by a mid-month date; both sides normalise to the 1st.
        var found = monthlyRepository.findByDeviceIdAndMonth(deviceId, DAY.withDayOfMonth(20)).orElseThrow();

        assertThat(found.getSummaryMonth()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(found.getDaysCovered()).isEqualTo(31);
        assertThat(found.getReadingCount()).isEqualTo(3100L);
        assertThat(found.getCategoryBreakdown()).isEqualTo(new AqiCategoryBreakdown(40, 55, 3, 2, 0, 0));
        assertThat(monthlyRepository.existsByDeviceIdAndMonth(deviceId, LocalDate.of(2026, 5, 1))).isTrue();
        assertThat(monthlyRepository.existsByDeviceIdAndMonth(deviceId, LocalDate.of(2026, 6, 1))).isFalse();
    }

    private static DeviceDailySummary daily(UUID deviceId, LocalDate date, long readings) {
        return new DeviceDailySummary(
                new DeviceId(deviceId), date,
                MetricStats.of(450.0, 400.0, 500.0),
                MetricStats.of(12.0, 8.0, 40.0),
                MetricStats.of(23.5, 20.0, 27.0),
                MetricStats.of(52.0, 45.0, 60.0),
                40.0, PEAK_AT, 55, AqiCategory.MODERATE,
                new AqiCategoryBreakdown(40, 55, 3, 2, 0, 0),
                readings, -3.5);
    }

    private static DeviceMonthlySummary monthly(UUID deviceId, LocalDate month) {
        return new DeviceMonthlySummary(
                new DeviceId(deviceId), month,
                MetricStats.of(450.0, 400.0, 500.0),
                MetricStats.of(12.0, 8.0, 40.0),
                MetricStats.of(23.5, 20.0, 27.0),
                MetricStats.of(52.0, 45.0, 60.0),
                40.0, PEAK_AT, 55, AqiCategory.MODERATE,
                new AqiCategoryBreakdown(40, 55, 3, 2, 0, 0),
                3100L, 31, -3.5);
    }
}
