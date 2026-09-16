package com.claircore.analytics.application.internal.commandservices;

import com.claircore.analytics.domain.model.aggregates.DeviceDailySummary;
import com.claircore.analytics.domain.model.aggregates.DeviceMonthlySummary;
import com.claircore.analytics.domain.model.commands.GenerateMonthlySummaryCommand;
import com.claircore.analytics.domain.model.valueobjects.AqiCategory;
import com.claircore.analytics.domain.model.valueobjects.AqiCategoryBreakdown;
import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.domain.model.valueobjects.MetricStats;
import com.claircore.analytics.domain.repositories.DeviceDailySummaryRepository;
import com.claircore.analytics.domain.repositories.DeviceMonthlySummaryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlySummaryCommandServiceImplTest {

    private static final YearMonth MONTH = YearMonth.of(2026, 5);
    private static final LocalDate FIRST = LocalDate.of(2026, 5, 1);
    private static final Instant PEAK_AT = Instant.parse("2026-05-16T14:30:00Z");

    @Mock
    private DeviceDailySummaryRepository dailySummaryRepository;

    @Mock
    private DeviceMonthlySummaryRepository monthlySummaryRepository;

    @InjectMocks
    private MonthlySummaryCommandServiceImpl commandService;

    @Test
    void cascadesTheWholeMonthInclusiveOfItsLastDay() {
        when(dailySummaryRepository.findAllByDateBetween(any(), any())).thenReturn(List.of());

        commandService.handle(new GenerateMonthlySummaryCommand(MONTH));

        verify(dailySummaryRepository).findAllByDateBetween(FIRST, LocalDate.of(2026, 5, 31));
    }

    @Test
    void averagesAreWeightedByReadingCountNotAveragedAgain() {
        UUID deviceId = UUID.randomUUID();
        // 10 readings averaging 400, then 90 averaging 500: the weighted mean is 490, not 450.
        when(dailySummaryRepository.findAllByDateBetween(any(), any())).thenReturn(List.of(
                daily(deviceId, FIRST, 10L, 400.0, 50),
                daily(deviceId, FIRST.plusDays(1), 90L, 500.0, 50)));
        when(monthlySummaryRepository.findByDeviceIdAndMonth(deviceId, FIRST.minusMonths(1)))
                .thenReturn(Optional.empty());

        int written = commandService.handle(new GenerateMonthlySummaryCommand(MONTH));

        assertThat(written).isEqualTo(1);
        var captor = ArgumentCaptor.forClass(DeviceMonthlySummary.class);
        verify(monthlySummaryRepository).save(captor.capture());
        var summary = captor.getValue();
        assertThat(summary.getCo2().avg()).isCloseTo(490.0, within(0.001));
        assertThat(summary.getReadingCount()).isEqualTo(100L);
        assertThat(summary.getDaysCovered()).isEqualTo(2);
        assertThat(summary.getSummaryMonth()).isEqualTo(FIRST);
    }

    @Test
    void extremesAreExtremesOfExtremesAndBreakdownsAreSummed() {
        UUID deviceId = UUID.randomUUID();
        when(dailySummaryRepository.findAllByDateBetween(any(), any())).thenReturn(List.of(
                daily(deviceId, FIRST, 10L, 400.0, 50),
                daily(deviceId, FIRST.plusDays(1), 90L, 500.0, 50)));
        when(monthlySummaryRepository.findByDeviceIdAndMonth(any(), any())).thenReturn(Optional.empty());

        commandService.handle(new GenerateMonthlySummaryCommand(MONTH));

        var captor = ArgumentCaptor.forClass(DeviceMonthlySummary.class);
        verify(monthlySummaryRepository).save(captor.capture());
        var summary = captor.getValue();
        assertThat(summary.getCo2().min()).isEqualTo(350.0);
        assertThat(summary.getCo2().max()).isEqualTo(550.0);
        assertThat(summary.getCategoryBreakdown()).isEqualTo(new AqiCategoryBreakdown(20, 0, 0, 0, 0, 0));
    }

    @Test
    void recomputesAMonthInsteadOfSkippingIt() {
        UUID deviceId = UUID.randomUUID();
        when(dailySummaryRepository.findAllByDateBetween(any(), any()))
                .thenReturn(List.of(daily(deviceId, FIRST, 10L, 400.0, 50)));

        assertThat(commandService.handle(new GenerateMonthlySummaryCommand(MONTH))).isEqualTo(1);
        verify(monthlySummaryRepository).save(any());
    }

    private static DeviceDailySummary daily(
            UUID deviceId, LocalDate date, long readings, double co2Avg, int aqi) {
        return new DeviceDailySummary(
                new DeviceId(deviceId), date,
                MetricStats.of(co2Avg, co2Avg - 50, co2Avg + 50),
                MetricStats.of(12.0, 8.0, 40.0),
                MetricStats.of(23.5, 20.0, 27.0),
                MetricStats.of(52.0, 45.0, 60.0),
                40.0, PEAK_AT, aqi, AqiCategory.GOOD,
                new AqiCategoryBreakdown(10, 0, 0, 0, 0, 0),
                readings, null);
    }
}
