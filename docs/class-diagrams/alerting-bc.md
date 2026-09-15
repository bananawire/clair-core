# Alerting Bounded Context Class Diagrams

This document contains both the unified Bounded Context class diagram and the layered individual diagrams.

## Unified Class Diagram

```mermaid
---
title: DDD Class Diagram - Alerting Bounded Context (4 Layers)
---

classDiagram

namespace interfaces {
    class AlertController {
        -AlertQueryService alertQueryService
        +getAlertsByDevice(deviceId, page, size) ResponseEntity
        +getAlertsBySpace(spaceId, page, size) ResponseEntity
        +getDailySummary(spaceId) ResponseEntity
    }
    class AlertingContextFacade {
        <<interface>>
        +findAlertDetailsById(alertId) Optional~AlertDetails~
    }
}

namespace application {
    class AlertCommandServiceImpl {
        -AlertRepository alertRepository
        -ExternalAlertingThresholdService externalThresholdService
        -ExternalAlertingDeviceService externalDeviceService
        -AlertIncidentsChangedPublisher alertIncidentsChangedPublisher
        +handle(EvaluateTelemetryForAlertsCommand) void
    }
    class AlertQueryServiceImpl {
        -AlertRepository alertRepository
        -ExternalAlertingDeviceService externalDeviceService
        +handle(GetAlertsByDeviceQuery) Page~Alert~
        +handle(GetAlertsBySpaceQuery) Page~Alert~
        +handle(GetAlertsByOwnerQuery) List~DailyAlertCount~
    }
    class AlertingContextFacadeImpl {
        -AlertRepository alertRepository
        +findAlertDetailsById(alertId) Optional~AlertDetails~
    }
    class AlertingTelemetryRecordedEventListener {
        -AlertCommandService alertCommandService
        +onTelemetryRecorded(TelemetryRecordedEvent) void
    }
    class ExternalAlertingDeviceService {
        <<interface>>
        +fetchSpaceIdByDeviceId(deviceId) Optional~UUID~
        +fetchSpaceNameBySpaceId(spaceId) Optional~String~
        +fetchDeviceNameByDeviceId(deviceId) Optional~String~
        +fetchHardwareIdByDeviceId(deviceId) Optional~String~
    }
    class ExternalAlertingThresholdService {
        <<interface>>
        +fetchEnabledThresholdsByDeviceId(deviceId) List~DeviceMetricThresholdConfiguration~
    }
    class EvaluateTelemetryForAlertsCommand {
        +UUID deviceId
        +BigDecimal co2
        +BigDecimal pm25
        +BigDecimal temperature
        +BigDecimal humidity
        +Instant occurredAt
    }
}

namespace domain {
    class TelemetryRecordedEvent
    class AlertIncidentChangedEvent
    class Alert {
        -UUID id
        -UUID deviceId
        -UUID spaceId
        -MetricType metric
        -BigDecimal thresholdValue
        -BigDecimal actualValue
        -String message
        -AlertStatus status
        -AlertSeverity severity
        -String spaceName
        -String deviceName
        -Instant occurredAt
        -Instant resolvedAt
        +acknowledge() void
        +resolve(resolvedAt) void
    }
    class MetricType {
        <<enumeration>>
        CO2
        PM25
        TEMPERATURE
        HUMIDITY
    }
    class AlertSeverity {
        <<enumeration>>
        LOW
        WARNING
        CRITICAL
    }
    class AlertStatus {
        <<enumeration>>
        ACTIVE
        ACKNOWLEDGED
        RESOLVED
    }
    class DailyAlertCount {
        +LocalDate date
        +Long count
    }
    class AlertRepository {
        <<interface>>
        +save(alert) Alert
        +findById(id) Optional~Alert~
        +findByDeviceIdAndMetricAndStatusIn(deviceId, metric, statuses) Optional~Alert~
        +findBySpaceIdAndStatus(spaceId, status, pageable) Page~Alert~
        +findFirstByDeviceIdAndMetricAndStatusIn(deviceId, metric, statuses) Optional~Alert~
    }
}

namespace infrastructure {
    class JpaAlertRepository {
        <<interface>>
        +findFirstByDeviceIdAndMetricAndStatusIn(deviceId, metric, statuses)
    }
    class AlertIncidentsChangedPublisher {
        -ApplicationEventPublisher eventPublisher
        -EdgeEventPublisher edgeEventPublisher
        +publish(AlertIncidentChangedIntegrationEvent)
    }
}

AlertController --> AlertQueryServiceImpl : uses
AlertingTelemetryRecordedEventListener --> AlertCommandServiceImpl : uses
AlertingTelemetryRecordedEventListener --> TelemetryRecordedEvent : @EventListener
AlertIncidentsChangedPublisher --> ApplicationEventPublisher : publishes AlertIncidentChangedEvent
AlertIncidentsChangedPublisher --> EdgeEventPublisher : notifies edge via HTTP

AlertCommandServiceImpl --> Alert : creates/updates
AlertCommandServiceImpl --> AlertRepository : uses
AlertCommandServiceImpl --> ExternalAlertingDeviceService : uses
AlertCommandServiceImpl --> ExternalAlertingThresholdService : uses
AlertCommandServiceImpl --> AlertIncidentsChangedPublisher : publishes

AlertQueryServiceImpl --> AlertRepository : uses

AlertingContextFacadeImpl ..|> AlertingContextFacade : implements
AlertingContextFacadeImpl --> AlertRepository : uses

Alert --> MetricType : contains
Alert --> AlertSeverity : contains
Alert --> AlertStatus : contains

JpaAlertRepository ..|> AlertRepository : implements
```

---

## Layered Diagrams

### 1. Interfaces Layer

```mermaid
classDiagram
class AlertController {
    -AlertQueryService alertQueryService
    +getAlertsByDevice(deviceId, page, size) ResponseEntity
    +getAlertsBySpace(spaceId, page, size) ResponseEntity
    +getDailySummary(spaceId) ResponseEntity
}
class AlertingContextFacade {
    <<interface>>
    +findAlertDetailsById(alertId) Optional~AlertDetails~
}
```

### 2. Application Layer

```mermaid
classDiagram
class AlertCommandServiceImpl {
    -AlertRepository alertRepository
    -ExternalAlertingThresholdService externalThresholdService
    -ExternalAlertingDeviceService externalDeviceService
    -AlertIncidentsChangedPublisher alertIncidentsChangedPublisher
    +handle(EvaluateTelemetryForAlertsCommand) void
}
class AlertQueryServiceImpl {
    -AlertRepository alertRepository
    -ExternalAlertingDeviceService externalDeviceService
    +handle(GetAlertsByDeviceQuery) Page~Alert~
    +handle(GetAlertsBySpaceQuery) Page~Alert~
    +handle(GetAlertsByOwnerQuery) List~DailyAlertCount~
}
class AlertingContextFacadeImpl {
    -AlertRepository alertRepository
    +findAlertDetailsById(alertId) Optional~AlertDetails~
}
class AlertingTelemetryRecordedEventListener {
    -AlertCommandService alertCommandService
    +onTelemetryRecorded(TelemetryRecordedEvent) void
}
class ExternalAlertingDeviceService {
    <<interface>>
    +fetchSpaceIdByDeviceId(deviceId) Optional~UUID~
    +fetchSpaceNameBySpaceId(spaceId) Optional~String~
    +fetchDeviceNameByDeviceId(deviceId) Optional~String~
    +fetchHardwareIdByDeviceId(deviceId) Optional~String~
}
class ExternalAlertingThresholdService {
    <<interface>>
    +fetchEnabledThresholdsByDeviceId(deviceId) List~DeviceMetricThresholdConfiguration~
}
class EvaluateTelemetryForAlertsCommand {
    +UUID deviceId
    +BigDecimal co2
    +BigDecimal pm25
    +BigDecimal temperature
    +BigDecimal humidity
    +Instant occurredAt
}

AlertingTelemetryRecordedEventListener --> AlertCommandServiceImpl : uses
AlertingTelemetryRecordedEventListener --> TelemetryRecordedEvent : @EventListener
AlertIncidentsChangedPublisher --> ApplicationEventPublisher : publishes AlertIncidentChangedEvent
AlertIncidentsChangedPublisher --> EdgeEventPublisher : notifies edge via HTTP
AlertCommandServiceImpl --> ExternalAlertingDeviceService : uses
AlertCommandServiceImpl --> ExternalAlertingThresholdService : uses
AlertingContextFacadeImpl ..|> AlertingContextFacade : implements
```

### 3. Domain Layer

```mermaid
classDiagram
class Alert {
    -UUID id
    -UUID deviceId
    -UUID spaceId
    -MetricType metric
    -BigDecimal thresholdValue
    -BigDecimal actualValue
    -String message
    -AlertStatus status
    -AlertSeverity severity
    -String spaceName
    -String deviceName
    -Instant occurredAt
    -Instant resolvedAt
    +acknowledge() void
    +resolve(resolvedAt) void
}
class MetricType {
    <<enumeration>>
    CO2
    PM25
    TEMPERATURE
    HUMIDITY
}
class AlertSeverity {
    <<enumeration>>
    LOW
    WARNING
    CRITICAL
}
class AlertStatus {
    <<enumeration>>
    ACTIVE
    ACKNOWLEDGED
    RESOLVED
}
class DailyAlertCount {
    +LocalDate date
    +Long count
}
class AlertRepository {
    <<interface>>
    +save(alert) Alert
    +findById(id) Optional~Alert~
    +findByDeviceIdAndMetricAndStatusIn(deviceId, metric, statuses) Optional~Alert~
    +findBySpaceIdAndStatus(spaceId, status, pageable) Page~Alert~
    +findFirstByDeviceIdAndMetricAndStatusIn(deviceId, metric, statuses) Optional~Alert~
}

Alert --> MetricType : contains
Alert --> AlertSeverity : contains
Alert --> AlertStatus : contains
```

### 4. Infrastructure Layer

```mermaid
classDiagram
class JpaAlertRepository {
    <<interface>>
    +findFirstByDeviceIdAndMetricAndStatusIn(deviceId, metric, statuses)
}
class AlertIncidentsChangedPublisher {
    -ApplicationEventPublisher eventPublisher
    -EdgeEventPublisher edgeEventPublisher
    +publish(AlertIncidentChangedIntegrationEvent)
}
```
