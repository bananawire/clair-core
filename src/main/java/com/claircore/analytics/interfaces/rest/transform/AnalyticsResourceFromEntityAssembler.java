package com.claircore.analytics.interfaces.rest.transform;

import com.claircore.analytics.domain.model.aggregates.DeviceDailySummary;
import com.claircore.analytics.domain.model.aggregates.DeviceMonthlySummary;
import com.claircore.analytics.domain.model.valueobjects.AqiCategory;
import com.claircore.analytics.domain.model.valueobjects.AqiCategoryBreakdown;
import com.claircore.analytics.domain.model.valueobjects.KpiDashboardMetrics;
import com.claircore.analytics.domain.model.valueobjects.KpiTrendPoint;
import com.claircore.analytics.domain.model.valueobjects.MetricStats;
import com.claircore.analytics.interfaces.rest.resources.DailyReportResponse;
import com.claircore.analytics.interfaces.rest.resources.DashboardMetricsResponse;
import com.claircore.analytics.interfaces.rest.resources.MonthlyReportResponse;
import com.claircore.analytics.interfaces.rest.resources.TrendChartResponse;

import java.util.Arrays;
import java.util.List;

public class AnalyticsResourceFromEntityAssembler {

    private AnalyticsResourceFromEntityAssembler() {}

    public static DailyReportResponse toDailyReportResponse(DeviceDailySummary s) {
        return new DailyReportResponse(
                s.getDeviceId().value(),
                s.getSummaryDate(),
                toStatsView(s.getCo2()),
                toStatsView(s.getPm2_5()),
                toStatsView(s.getTemperature()),
                toStatsView(s.getHumidity()),
                s.getPeakPm2_5(),
                s.getPeakPm2_5At(),
                s.getAverageAqi(),
                s.getDominantAqiCategory().name(),
                toCategoryShares(s.getCategoryBreakdown()),
                s.getReadingCount(),
                s.getAqiDeltaPct()
        );
    }

    public static MonthlyReportResponse toMonthlyReportResponse(DeviceMonthlySummary s) {
        return new MonthlyReportResponse(
                s.getDeviceId().value(),
                s.getSummaryMonth(),
                toStatsView(s.getCo2()),
                toStatsView(s.getPm2_5()),
                toStatsView(s.getTemperature()),
                toStatsView(s.getHumidity()),
                s.getPeakPm2_5(),
                s.getPeakPm2_5At(),
                s.getAverageAqi(),
                s.getDominantAqiCategory().name(),
                toCategoryShares(s.getCategoryBreakdown()),
                s.getReadingCount(),
                s.getDaysCovered(),
                s.getAqiDeltaPct()
        );
    }

    private static DailyReportResponse.MetricStatsView toStatsView(MetricStats stats) {
        return new DailyReportResponse.MetricStatsView(stats.avg(), stats.min(), stats.max());
    }

    private static List<DailyReportResponse.CategoryShare> toCategoryShares(AqiCategoryBreakdown breakdown) {
        long total = breakdown.total();
        return Arrays.stream(AqiCategory.values())
                .map(category -> {
                    long count = breakdown.countOf(category);
                    double percentage = total > 0 ? (count * 100.0) / total : 0.0;
                    return new DailyReportResponse.CategoryShare(category.name(), count, percentage);
                })
                .toList();
    }

    public static DashboardMetricsResponse toDashboardResponse(KpiDashboardMetrics metrics) {
        return new DashboardMetricsResponse(
                metrics.aqi().value(),
                metrics.aqi().category().name(),
                metrics.averageCo2(),
                metrics.averagePm2_5(),
                metrics.averageTemperature(),
                metrics.averageHumidity(),
                metrics.co2Trend().deltaPercentage(),
                metrics.pm2_5Trend().deltaPercentage(),
                metrics.temperatureTrend().deltaPercentage(),
                metrics.humidityTrend().deltaPercentage(),
                metrics.calculatedAt()
        );
    }

    public static TrendChartResponse toTrendChartResponse(List<KpiTrendPoint> points) {
        var dataPoints = points.stream()
                .map(p -> new TrendChartResponse.TrendDataPoint(
                        p.timestamp(),
                        p.aqiValue(),
                        p.co2(),
                        p.pm2_5(),
                        p.temperature(),
                        p.humidity()
                ))
                .toList();
        return new TrendChartResponse(dataPoints);
    }
}
