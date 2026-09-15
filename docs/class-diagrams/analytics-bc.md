# Analytics Bounded Context Class Diagrams

This document contains both the unified Bounded Context class diagram and the layered individual diagrams.

## Unified Class Diagram

```mermaid
---
title: DDD Class Diagram - Analytics Bounded Context (4 Layers)
---

classDiagram

namespace interfaces {
    class AnalyticsController {
        -KpiDashboardMetricsQueryService kpiDashboardMetricsQueryService
        -KpiHistoricalTrendQueryService kpiHistoricalTrendQueryService
        -AnalyticsSseService analyticsSseService
        +getLiveMetrics(deviceId) ResponseEntity
        +streamLiveMetrics(deviceId) SseEmitter
        +getHistoricalMetrics(deviceId, period, startDate, endDate) ResponseEntity
        +getTrends(deviceId, period, startDate, endDate, limit) ResponseEntity
    }
    class AnalyticsOverviewController {
        -OverviewDashboardQueryService overviewDashboardQueryService
        +getOverviewDashboard() ResponseEntity
    }
    class ReportController {
        -DailyReportQueryService dailyReportQueryService
        -MonthlyReportQueryService monthlyReportQueryService
        -ExternalDeviceService externalDeviceService
        -ExternalBillingService externalBillingService
        +getDailyReport(httpRequest, deviceId, date) ResponseEntity
        +getMonthlyReport(httpRequest, deviceId, month) ResponseEntity
    }
    class AnalyticsContextFacade {
        <<interface>>
    }
}

namespace application {
    class KpiLiveMetricsCommandServiceImpl {
        -KpiLiveMetricsCache kpiLiveMetricsCache
        -AnalyticsSseService analyticsSseService
        +handle(ProcessTelemetryAnalyticCommand) void
    }
    class KpiDashboardMetricsQueryServiceImpl {
        -KpiLiveMetricsCache kpiLiveMetricsCache
        -DeviceAnalyticsSnapshotRepository deviceAnalyticsSnapshotRepository
        -ExternalEvaluationService externalEvaluationService
        +handle(GetDashboardMetricsQuery) Optional~KpiDashboardMetrics~
    }
    class KpiHistoricalTrendQueryServiceImpl {
        -DeviceAnalyticsSnapshotRepository deviceAnalyticsSnapshotRepository
        -ExternalEvaluationService externalEvaluationService
        -TrendAnalysisDomainService trendAnalysisDomainService
        +handle(GetHistoricalTrendQuery) List~KpiTrendPoint~
    }
    class OverviewDashboardQueryServiceImpl {
        -ExternalDeviceService externalDeviceService
        -KpiLiveMetricsCache kpiLiveMetricsCache
        +handle(GetOverviewDashboardQuery) OverviewDashboardSnapshot
    }
    class DailyReportQueryServiceImpl {
        -DeviceDailySummaryRepository dailySummaryRepository
        +handle(GetDailyReportQuery) Optional~DeviceDailySummary~
    }
    class MonthlyReportQueryServiceImpl {
        -DeviceMonthlySummaryRepository monthlySummaryRepository
        +handle(GetMonthlyReportQuery) Optional~DeviceMonthlySummary~
    }
    class DailyReportAggregationService {
        -JdbcTemplate jdbcTemplate
        -DeviceDailySummaryRepository dailySummaryRepository
        -AqiCalculationDomainService aqiCalculationDomainService
        -ZoneId reportZone
        +aggregatePreviousDay() void
        +generateForDate(date) void
    }
    class MonthlyReportAggregationService {
        -DeviceDailySummaryRepository dailySummaryRepository
        -DeviceMonthlySummaryRepository monthlySummaryRepository
        -ZoneId reportZone
        +aggregatePreviousMonth() void
        +generateForMonth(month) void
    }
    class AnalyticsContextFacadeImpl {
        <<interface>>
    }
    class TelemetryAnalyticEventListener {
        -KpiLiveMetricsCommandService kpiLiveMetricsCommandService
        +onTelemetryRecorded(TelemetryRecordedEvent) void
    }
    class KpiLiveMetricsCache {
        -ConcurrentHashMap~DeviceId, DeviceMetricsSnapshot~ cache
        +get(deviceId) Optional~DeviceMetricsSnapshot~
        +put(deviceId, snapshot) void
    }
    class AnalyticsSseService {
        -ConcurrentHashMap~UUID, List~SseEmitter~~ emitters
        +registerClient(deviceId) SseEmitter
        +broadcast(deviceId, data) void
    }
    class SnapshotAggregationScheduler {
        -MetricsAggregationDomainService metricsAggregationDomainService
        -DeviceAnalyticsSnapshotRepository deviceAnalyticsSnapshotRepository
        -ExternalDeviceService externalDeviceService
        -ExternalEvaluationService externalEvaluationService
        -AqiCalculationDomainService aqiCalculationDomainService
        +aggregateSnapshots() void
    }
    class ExternalDeviceService {
        <<interface>>
        +fetchAllActiveDevices() List
    }
    class ExternalEvaluationService {
        <<interface>>
        +fetchHourlyTelemetryAggregation(start, end) List
        +getLatestEvaluationRecordedAt(deviceId) Optional~Instant~
    }
    class ExternalBillingService {
        <<interface>>
        +canAccessMonthlyReports(userId) boolean
    }
}

namespace domain {
    class TelemetryRecordedEvent
    class DeviceAnalyticsSnapshot {
        -UUID id
        -DeviceId deviceId
        -Instant timeWindowStart
        -Instant timeWindowEnd
        -Double averageCo2
        -Double averagePm2_5
        -Double averageTemperature
        -Double averageHumidity
        -AirQualityIndex calculatedAqi
    }
    class DeviceDailySummary {
        -UUID id
        -DeviceId deviceId
        -LocalDate summaryDate
        -MetricStats co2
        -MetricStats pm2_5
        -MetricStats temperature
        -MetricStats humidity
        -Double peakPm2_5
        -Instant peakPm2_5At
        -Integer averageAqi
        -AqiCategory dominantAqiCategory
        -AqiCategoryBreakdown categoryBreakdown
        -long readingCount
        -Double aqiDeltaPct
    }
    class DeviceMonthlySummary {
        -UUID id
        -DeviceId deviceId
        -LocalDate summaryMonth
        -MetricStats co2
        -MetricStats pm2_5
        -MetricStats temperature
        -MetricStats humidity
        -Double peakPm2_5
        -Instant peakPm2_5At
        -Integer averageAqi
        -AqiCategory dominantAqiCategory
        -AqiCategoryBreakdown categoryBreakdown
        -long readingCount
        -int daysCovered
        -Double aqiDeltaPct
    }
    class MetricStats {
        +Double avg
        +Double min
        +Double max
    }
    class AqiCategoryBreakdown {
        +Long good
        +Long moderate
        +Long unhealthyForSensitive
        +Long unhealthy
        +Long veryUnhealthy
        +Long hazardous
        +plus(other) AqiCategoryBreakdown
        +dominant() AqiCategory
    }
    class AirQualityIndex {
        +Double value
        +AqiCategory category
    }
    class AqiCategory {
        <<enumeration>>
        GOOD
        MODERATE
        UNHEALTHY_FOR_SENSITIVE_GROUPS
        UNHEALTHY
        VERY_UNHEALTHY
        HAZARDOUS
    }
    class DeviceId {
        +UUID value
    }
    class KpiDashboardMetrics {
        +DeviceId deviceId
        +Double currentCo2
        +Double currentPm2_5
        +Double currentTemperature
        +Double currentHumidity
        +AirQualityIndex calculatedAqi
    }
    class KpiTrendPoint {
        +Instant timestamp
        +Double co2
        +Double pm2_5
        +Double temperature
        +Double humidity
    }
    class GetDailyReportQuery {
        +DeviceId deviceId
        +LocalDate date
    }
    class GetMonthlyReportQuery {
        +DeviceId deviceId
        +LocalDate month
    }
    class AqiCalculationDomainService {
        <<interface>>
        +calculateAqi(pm2_5, co2) AirQualityIndex
    }
    class AqiCalculationDomainServiceImpl {
        +calculateAqi(pm2_5, co2) AirQualityIndex
    }
    class MetricsAggregationDomainService {
        <<interface>>
        +aggregate(snapshots) AggregatedMetrics
    }
    class MetricsAggregationDomainServiceImpl {
        -AqiCalculationDomainService aqiCalculationDomainService
        +aggregate(snapshots) AggregatedMetrics
    }
    class TrendAnalysisDomainService {
        <<interface>>
        +calculateTrend(currentValue, previousValue) MetricTrend
    }
    class TrendAnalysisDomainServiceImpl {
        +calculateTrend(currentValue, previousValue) MetricTrend
    }
    class DailyReportQueryService {
        <<interface>>
        +handle(GetDailyReportQuery) Optional~DeviceDailySummary~
    }
    class MonthlyReportQueryService {
        <<interface>>
        +handle(GetMonthlyReportQuery) Optional~DeviceMonthlySummary~
    }
    class DeviceAnalyticsSnapshotRepository {
        <<interface>>
        +save(snapshot) DeviceAnalyticsSnapshot
        +findByDeviceIdAndTimeWindowStartBetween(deviceId, start, end) List~DeviceAnalyticsSnapshot~
    }
}

namespace infrastructure {
    class JpaDeviceAnalyticsSnapshotRepository {
        <<interface>>
    }
    class DeviceDailySummaryRepository {
        <<interface>>
        +findByDeviceIdAndDate(deviceId, date) Optional~DeviceDailySummary~
        +findLatestByDeviceId(deviceId) Optional~DeviceDailySummary~
        +findByDeviceIdAndDateBetween(deviceId, start, end) List~DeviceDailySummary~
        +existsByDeviceIdAndDate(deviceId, date) boolean
        +findAllByDateBetween(start, end) List~DeviceDailySummary~
    }
    class DeviceMonthlySummaryRepository {
        <<interface>>
        +findByDeviceIdAndMonth(deviceId, month) Optional~DeviceMonthlySummary~
        +existsByDeviceIdAndMonth(deviceId, month) boolean
    }
}

AnalyticsController --> KpiDashboardMetricsQueryServiceImpl : uses
AnalyticsController --> KpiHistoricalTrendQueryServiceImpl : uses
AnalyticsController --> AnalyticsSseService : uses

AnalyticsOverviewController --> OverviewDashboardQueryServiceImpl : uses

ReportController --> DailyReportQueryService : uses
ReportController --> MonthlyReportQueryService : uses
ReportController --> ExternalDeviceService : uses
ReportController --> ExternalBillingService : uses

KpiLiveMetricsCommandServiceImpl --> KpiLiveMetricsCache : uses
KpiLiveMetricsCommandServiceImpl --> AnalyticsSseService : uses

KpiDashboardMetricsQueryServiceImpl --> DeviceAnalyticsSnapshotRepository : uses
KpiDashboardMetricsQueryServiceImpl --> KpiLiveMetricsCache : uses
KpiDashboardMetricsQueryServiceImpl --> ExternalEvaluationService : uses

KpiHistoricalTrendQueryServiceImpl --> DeviceAnalyticsSnapshotRepository : uses
KpiHistoricalTrendQueryServiceImpl --> ExternalEvaluationService : uses
KpiHistoricalTrendQueryServiceImpl --> TrendAnalysisDomainService : uses

OverviewDashboardQueryServiceImpl --> ExternalDeviceService : uses
OverviewDashboardQueryServiceImpl --> KpiLiveMetricsCache : uses

DailyReportQueryServiceImpl ..|> DailyReportQueryService : implements
DailyReportQueryServiceImpl --> DeviceDailySummaryRepository : uses

MonthlyReportQueryServiceImpl ..|> MonthlyReportQueryService : implements
MonthlyReportQueryServiceImpl --> DeviceMonthlySummaryRepository : uses

DailyReportAggregationService --> DeviceDailySummaryRepository : uses
DailyReportAggregationService --> AqiCalculationDomainService : uses

MonthlyReportAggregationService --> DeviceDailySummaryRepository : uses
MonthlyReportAggregationService --> DeviceMonthlySummaryRepository : uses

TelemetryAnalyticEventListener --> KpiLiveMetricsCommandServiceImpl : uses
TelemetryAnalyticEventListener --> TelemetryRecordedEvent : @EventListener

SnapshotAggregationScheduler --> MetricsAggregationDomainService : uses
SnapshotAggregationScheduler --> DeviceAnalyticsSnapshotRepository : uses
SnapshotAggregationScheduler --> ExternalDeviceService : uses
SnapshotAggregationScheduler --> ExternalEvaluationService : uses
SnapshotAggregationScheduler --> AqiCalculationDomainService : uses

DeviceAnalyticsSnapshot --> DeviceId : contains
DeviceAnalyticsSnapshot --> AirQualityIndex : contains
AirQualityIndex --> AqiCategory : contains

DeviceDailySummary --> DeviceId : contains
DeviceDailySummary --> MetricStats : contains
DeviceDailySummary --> AqiCategory : contains
DeviceDailySummary --> AqiCategoryBreakdown : contains

DeviceMonthlySummary --> DeviceId : contains
DeviceMonthlySummary --> MetricStats : contains
DeviceMonthlySummary --> AqiCategory : contains
DeviceMonthlySummary --> AqiCategoryBreakdown : contains

AqiCalculationDomainServiceImpl ..|> AqiCalculationDomainService : implements
MetricsAggregationDomainServiceImpl ..|> MetricsAggregationDomainService : implements
TrendAnalysisDomainServiceImpl ..|> TrendAnalysisDomainService : implements
JpaDeviceAnalyticsSnapshotRepository ..|> DeviceAnalyticsSnapshotRepository : implements
```

---

## Layered Diagrams

### 1. Interfaces Layer

```mermaid
classDiagram
class AnalyticsController {
    -KpiDashboardMetricsQueryService kpiDashboardMetricsQueryService
    -KpiHistoricalTrendQueryService kpiHistoricalTrendQueryService
    -AnalyticsSseService analyticsSseService
    +getLiveMetrics(deviceId) ResponseEntity
    +streamLiveMetrics(deviceId) SseEmitter
    +getHistoricalMetrics(deviceId, period, startDate, endDate) ResponseEntity
    +getTrends(deviceId, period, startDate, endDate, limit) ResponseEntity
}
class AnalyticsOverviewController {
    -OverviewDashboardQueryService overviewDashboardQueryService
    +getOverviewDashboard() ResponseEntity
}
class ReportController {
    -DailyReportQueryService dailyReportQueryService
    -MonthlyReportQueryService monthlyReportQueryService
    -ExternalDeviceService externalDeviceService
    -ExternalBillingService externalBillingService
    +getDailyReport(httpRequest, deviceId, date) ResponseEntity
    +getMonthlyReport(httpRequest, deviceId, month) ResponseEntity
}
class AnalyticsContextFacade {
    <<interface>>
}
```

### 2. Application Layer

```mermaid
classDiagram
class KpiLiveMetricsCommandServiceImpl {
    -KpiLiveMetricsCache kpiLiveMetricsCache
    -AnalyticsSseService analyticsSseService
    +handle(ProcessTelemetryAnalyticCommand) void
}
class KpiDashboardMetricsQueryServiceImpl {
    -KpiLiveMetricsCache kpiLiveMetricsCache
    -DeviceAnalyticsSnapshotRepository deviceAnalyticsSnapshotRepository
    -ExternalEvaluationService externalEvaluationService
    +handle(GetDashboardMetricsQuery) Optional~KpiDashboardMetrics~
}
class KpiHistoricalTrendQueryServiceImpl {
    -DeviceAnalyticsSnapshotRepository deviceAnalyticsSnapshotRepository
    -ExternalEvaluationService externalEvaluationService
    -TrendAnalysisDomainService trendAnalysisDomainService
    +handle(GetHistoricalTrendQuery) List~KpiTrendPoint~
}
class OverviewDashboardQueryServiceImpl {
    -ExternalDeviceService externalDeviceService
    -KpiLiveMetricsCache kpiLiveMetricsCache
    +handle(GetOverviewDashboardQuery) OverviewDashboardSnapshot
}
class DailyReportQueryServiceImpl {
    -DeviceDailySummaryRepository dailySummaryRepository
    +handle(GetDailyReportQuery) Optional~DeviceDailySummary~
}
class MonthlyReportQueryServiceImpl {
    -DeviceMonthlySummaryRepository monthlySummaryRepository
    +handle(GetMonthlyReportQuery) Optional~DeviceMonthlySummary~
}
class DailyReportAggregationService {
    -JdbcTemplate jdbcTemplate
    -DeviceDailySummaryRepository dailySummaryRepository
    -AqiCalculationDomainService aqiCalculationDomainService
    -ZoneId reportZone
    +aggregatePreviousDay() void
    +generateForDate(date) void
}
class MonthlyReportAggregationService {
    -DeviceDailySummaryRepository dailySummaryRepository
    -DeviceMonthlySummaryRepository monthlySummaryRepository
    -ZoneId reportZone
    +aggregatePreviousMonth() void
    +generateForMonth(month) void
}
class AnalyticsContextFacadeImpl {
    <<interface>>
}
class TelemetryAnalyticEventListener {
    -KpiLiveMetricsCommandService kpiLiveMetricsCommandService
    +onTelemetryRecorded(TelemetryRecordedEvent) void
}
class KpiLiveMetricsCache {
    -ConcurrentHashMap~DeviceId, DeviceMetricsSnapshot~ cache
    +get(deviceId) Optional~DeviceMetricsSnapshot~
    +put(deviceId, snapshot) void
}
class AnalyticsSseService {
    -ConcurrentHashMap~UUID, List~SseEmitter~~ emitters
    +registerClient(deviceId) SseEmitter
    +broadcast(deviceId, data) void
}
class SnapshotAggregationScheduler {
    -MetricsAggregationDomainService metricsAggregationDomainService
    -DeviceAnalyticsSnapshotRepository deviceAnalyticsSnapshotRepository
    -ExternalDeviceService externalDeviceService
    -ExternalEvaluationService externalEvaluationService
    -AqiCalculationDomainService aqiCalculationDomainService
    +aggregateSnapshots() void
}
class ExternalDeviceService {
    <<interface>>
    +fetchAllActiveDevices() List
}
class ExternalEvaluationService {
    <<interface>>
    +fetchHourlyTelemetryAggregation(start, end) List
    +getLatestEvaluationRecordedAt(deviceId) Optional~Instant~
}
class ExternalBillingService {
    <<interface>>
    +canAccessMonthlyReports(userId) boolean
}

KpiLiveMetricsCommandServiceImpl --> KpiLiveMetricsCache : uses
KpiLiveMetricsCommandServiceImpl --> AnalyticsSseService : uses
OverviewDashboardQueryServiceImpl --> ExternalDeviceService : uses
OverviewDashboardQueryServiceImpl --> KpiLiveMetricsCache : uses
TelemetryAnalyticEventListener --> KpiLiveMetricsCommandServiceImpl : uses
TelemetryAnalyticEventListener --> TelemetryRecordedEvent : @EventListener
KpiDashboardMetricsQueryServiceImpl --> KpiLiveMetricsCache : uses
KpiDashboardMetricsQueryServiceImpl --> ExternalEvaluationService : uses
KpiHistoricalTrendQueryServiceImpl --> ExternalEvaluationService : uses
SnapshotAggregationScheduler --> ExternalDeviceService : uses
SnapshotAggregationScheduler --> ExternalEvaluationService : uses
```

### 3. Domain Layer

```mermaid
classDiagram
class DeviceAnalyticsSnapshot {
    -UUID id
    -DeviceId deviceId
    -Instant timeWindowStart
    -Instant timeWindowEnd
    -Double averageCo2
    -Double averagePm2_5
    -Double averageTemperature
    -Double averageHumidity
    -AirQualityIndex calculatedAqi
}
class DeviceDailySummary {
    -UUID id
    -DeviceId deviceId
    -LocalDate summaryDate
    -MetricStats co2
    -MetricStats pm2_5
    -MetricStats temperature
    -MetricStats humidity
    -Double peakPm2_5
    -Instant peakPm2_5At
    -Integer averageAqi
    -AqiCategory dominantAqiCategory
    -AqiCategoryBreakdown categoryBreakdown
    -long readingCount
    -Double aqiDeltaPct
}
class DeviceMonthlySummary {
    -UUID id
    -DeviceId deviceId
    -LocalDate summaryMonth
    -MetricStats co2
    -MetricStats pm2_5
    -MetricStats temperature
    -MetricStats humidity
    -Double peakPm2_5
    -Instant peakPm2_5At
    -Integer averageAqi
    -AqiCategory dominantAqiCategory
    -AqiCategoryBreakdown categoryBreakdown
    -long readingCount
    -int daysCovered
    -Double aqiDeltaPct
}
class MetricStats {
    +Double avg
    +Double min
    +Double max
}
class AqiCategoryBreakdown {
    +Long good
    +Long moderate
    +Long unhealthyForSensitive
    +Long unhealthy
    +Long veryUnhealthy
    +Long hazardous
    +plus(other) AqiCategoryBreakdown
    +dominant() AqiCategory
}
class AirQualityIndex {
    +Double value
    +AqiCategory category
}
class AqiCategory {
    <<enumeration>>
    GOOD
    MODERATE
    UNHEALTHY_FOR_SENSITIVE_GROUPS
    UNHEALTHY
    VERY_UNHEALTHY
    HAZARDOUS
}
class DeviceId {
    +UUID value
}
class KpiDashboardMetrics {
    +DeviceId deviceId
    +Double currentCo2
    +Double currentPm2_5
    +Double currentTemperature
    +Double currentHumidity
    +AirQualityIndex calculatedAqi
}
class KpiTrendPoint {
    +Instant timestamp
    +Double co2
    +Double pm2_5
    +Double temperature
    +Double humidity
}
class GetDailyReportQuery {
    +DeviceId deviceId
    +LocalDate date
}
class GetMonthlyReportQuery {
    +DeviceId deviceId
    +LocalDate month
}
class AqiCalculationDomainService {
    <<interface>>
    +calculateAqi(pm2_5, co2) AirQualityIndex
}
class AqiCalculationDomainServiceImpl {
    +calculateAqi(pm2_5, co2) AirQualityIndex
}
class MetricsAggregationDomainService {
    <<interface>>
    +aggregate(snapshots) AggregatedMetrics
}
class MetricsAggregationDomainServiceImpl {
    -AqiCalculationDomainService aqiCalculationDomainService
    +aggregate(snapshots) AggregatedMetrics
}
class TrendAnalysisDomainService {
    <<interface>>
    +calculateTrend(currentValue, previousValue) MetricTrend
}
class TrendAnalysisDomainServiceImpl {
    +calculateTrend(currentValue, previousValue) MetricTrend
}
class DailyReportQueryService {
    <<interface>>
    +handle(GetDailyReportQuery) Optional~DeviceDailySummary~
}
class MonthlyReportQueryService {
    <<interface>>
    +handle(GetMonthlyReportQuery) Optional~DeviceMonthlySummary~
}
class DeviceAnalyticsSnapshotRepository {
    <<interface>>
    +save(snapshot) DeviceAnalyticsSnapshot
    +findByDeviceIdAndTimeWindowStartBetween(deviceId, start, end) List~DeviceAnalyticsSnapshot~
}

DeviceAnalyticsSnapshot --> DeviceId : contains
DeviceAnalyticsSnapshot --> AirQualityIndex : contains
AirQualityIndex --> AqiCategory : contains

DeviceDailySummary --> DeviceId : contains
DeviceDailySummary --> MetricStats : contains
DeviceDailySummary --> AqiCategory : contains
DeviceDailySummary --> AqiCategoryBreakdown : contains

DeviceMonthlySummary --> DeviceId : contains
DeviceMonthlySummary --> MetricStats : contains
DeviceMonthlySummary --> AqiCategory : contains
DeviceMonthlySummary --> AqiCategoryBreakdown : contains

AqiCalculationDomainServiceImpl ..|> AqiCalculationDomainService : implements
MetricsAggregationDomainServiceImpl ..|> MetricsAggregationDomainService : implements
TrendAnalysisDomainServiceImpl ..|> TrendAnalysisDomainService : implements
```

### 4. Infrastructure Layer

```mermaid
classDiagram
class JpaDeviceAnalyticsSnapshotRepository {
    <<interface>>
}
class DeviceDailySummaryRepository {
    <<interface>>
    +findByDeviceIdAndDate(deviceId, date) Optional~DeviceDailySummary~
    +findLatestByDeviceId(deviceId) Optional~DeviceDailySummary~
    +findByDeviceIdAndDateBetween(deviceId, start, end) List~DeviceDailySummary~
    +existsByDeviceIdAndDate(deviceId, date) boolean
    +findAllByDateBetween(start, end) List~DeviceDailySummary~
}
class DeviceMonthlySummaryRepository {
    <<interface>>
    +findByDeviceIdAndMonth(deviceId, month) Optional~DeviceMonthlySummary~
    +existsByDeviceIdAndMonth(deviceId, month) boolean
}
```
