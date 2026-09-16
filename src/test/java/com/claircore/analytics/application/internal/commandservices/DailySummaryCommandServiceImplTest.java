package com.claircore.analytics.application.internal.commandservices;

import com.claircore.analytics.application.internal.outboundservices.acl.ExternalEvaluationService;
import com.claircore.analytics.domain.model.aggregates.DeviceDailySummary;
import com.claircore.analytics.domain.model.commands.GenerateDailySummaryCommand;
import com.claircore.analytics.domain.model.valueobjects.AqiCategory;
import com.claircore.analytics.domain.repositories.DeviceDailySummaryRepository;
import com.claircore.analytics.domain.services.AqiCalculator;
import com.claircore.evaluation.interfaces.acl.TelemetryReading;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailySummaryCommandServiceImplTest {

    private static final LocalDate DAY = LocalDate.of(2026, 5, 16);
    /** America/Lima is UTC-5, so the local day runs 05:00Z to 05:00Z the next morning. */
    private static final Instant WINDOW_START = Instant.parse("2026-05-16T05:00:00Z");
    private static final Instant WINDOW_END = Instant.parse("2026-05-17T05:00:00Z");

    @Mock
    private ExternalEvaluationService externalEvaluationService;

    @Mock
    private DeviceDailySummaryRepository dailySummaryRepository;

    @Mock private com.claircore.analytics.application.commandservices.MonthlySummaryCommandService monthlySummaries;

    private DailySummaryCommandServiceImpl commandService;

    @BeforeEach
    void setUp() {
        commandService = new DailySummaryCommandServiceImpl(
                externalEvaluationService,
                dailySummaryRepository,
                new AqiCalculator(),
                monthlySummaries,
                "America/Lima");
    }

    @Test
    void asksTheEvaluationFacadeForTheLocalDayNotForTheUtcDay() {
        when(externalEvaluationService.fetchReadings(any(), any())).thenReturn(List.of());

        commandService.handle(new GenerateDailySummaryCommand(DAY));

        verify(externalEvaluationService).fetchReadings(WINDOW_START, WINDOW_END);
    }

    @Test
    void summarisesEachDeviceSeparatelyWithTrueExtremesAndThePeakTimestamp() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        Instant peak = WINDOW_START.plusSeconds(3600);
        when(externalEvaluationService.fetchReadings(any(), any())).thenReturn(List.of(
                reading(first, 400.0, 8.0, 20.0, 45.0, WINDOW_START),
                reading(first, 500.0, 40.0, 26.0, 59.0, peak),
                reading(second, 450.0, 12.0, 23.0, 52.0, WINDOW_START)));
        when(dailySummaryRepository.findByDeviceIdAndDate(any(), eq(DAY.minusDays(1))))
                .thenReturn(Optional.empty());

        int written = commandService.handle(new GenerateDailySummaryCommand(DAY));

        assertThat(written).isEqualTo(2);
        var captor = ArgumentCaptor.forClass(DeviceDailySummary.class);
        verify(dailySummaryRepository, org.mockito.Mockito.times(2)).save(captor.capture());

        var firstSummary = captor.getAllValues().stream()
                .filter(s -> s.getDeviceId().value().equals(first)).findFirst().orElseThrow();
        assertThat(firstSummary.getReadingCount()).isEqualTo(2);
        assertThat(firstSummary.getCo2().avg()).isEqualTo(450.0);
        assertThat(firstSummary.getCo2().min()).isEqualTo(400.0);
        assertThat(firstSummary.getCo2().max()).isEqualTo(500.0);
        assertThat(firstSummary.getPeakPm2_5()).isEqualTo(40.0);
        assertThat(firstSummary.getPeakPm2_5At()).isEqualTo(peak);
        assertThat(firstSummary.getCategoryBreakdown().total()).isEqualTo(2);
        assertThat(firstSummary.getDominantAqiCategory()).isNotNull();
        assertThat(firstSummary.getAqiDeltaPct()).isNull();
    }

    @Test
    void recomputesADayInsteadOfSkippingIt() {
        UUID deviceId = UUID.randomUUID();
        when(externalEvaluationService.fetchReadings(any(), any()))
                .thenReturn(List.of(reading(deviceId, 450.0, 12.0, 23.0, 52.0, WINDOW_START)));

        int written = commandService.handle(new GenerateDailySummaryCommand(DAY));

        assertThat(written).isEqualTo(1);
        verify(dailySummaryRepository).save(any());
    }

    @Test
    void deltaIsThePercentageChangeAgainstThePreviousDaysAqi() {
        UUID deviceId = UUID.randomUUID();
        when(externalEvaluationService.fetchReadings(any(), any()))
                .thenReturn(List.of(reading(deviceId, 450.0, 12.0, 23.0, 52.0, WINDOW_START)));
        when(dailySummaryRepository.findByDeviceIdAndDate(deviceId, DAY.minusDays(1)))
                .thenReturn(Optional.of(previousDayWithAqi(deviceId, 50)));

        commandService.handle(new GenerateDailySummaryCommand(DAY));

        var captor = ArgumentCaptor.forClass(DeviceDailySummary.class);
        verify(dailySummaryRepository).save(captor.capture());
        var summary = captor.getValue();
        assertThat(summary.getAqiDeltaPct())
                .isEqualTo(((summary.getAverageAqi() - 50) * 100.0) / 50);
    }

    @Test
    void writesNothingWhenTheDayHasNoTelemetry() {
        when(externalEvaluationService.fetchReadings(any(), any())).thenReturn(List.of());

        assertThat(commandService.handle(new GenerateDailySummaryCommand(DAY))).isZero();
        verify(dailySummaryRepository, never()).save(any());
    }

    private static TelemetryReading reading(
            UUID deviceId, double co2, double pm25, double temp, double hum, Instant at) {
        return new TelemetryReading(deviceId, co2, pm25, temp, hum, at);
    }

    private static DeviceDailySummary previousDayWithAqi(UUID deviceId, int aqi) {
        return new DeviceDailySummary(
                new com.claircore.analytics.domain.model.valueobjects.DeviceId(deviceId),
                DAY.minusDays(1),
                com.claircore.analytics.domain.model.valueobjects.MetricStats.of(450.0, 400.0, 500.0),
                com.claircore.analytics.domain.model.valueobjects.MetricStats.of(12.0, 8.0, 40.0),
                com.claircore.analytics.domain.model.valueobjects.MetricStats.of(23.5, 20.0, 27.0),
                com.claircore.analytics.domain.model.valueobjects.MetricStats.of(52.0, 45.0, 60.0),
                40.0, WINDOW_START, aqi, AqiCategory.MODERATE,
                com.claircore.analytics.domain.model.valueobjects.AqiCategoryBreakdown.empty(),
                10L, null);
    }
}
