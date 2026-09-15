# Notifications Bounded Context Class Diagrams

This document contains both the unified Bounded Context class diagram and the layered individual diagrams.

## Unified Class Diagram

```mermaid
---
title: DDD Class Diagram - Notifications Bounded Context (4 Layers)
---

classDiagram

namespace interfaces {
    class NotificationController {
        +getUserNotifications(page) ResponseEntity
    }
    class NotificationsContextFacade {
        <<interface>>
        +sendWelcomeEmail(emailAddress) void
        +sendVerificationCode(email, code) void
    }
    class PushNotificationResponse {
        +UUID id
        +UUID userId
        +UUID alertId
        +String title
        +String message
        +String status
        +String errorMessage
        +Instant createdAt
    }
}

namespace application {
    class EmailCommandServiceImpl {
        -EmailDeliveryService emailDeliveryService
        -EmailLogPersistence emailLogPersistence
        +handle(SendVerificationCodeCommand) void
        +handle(SendWelcomeEmailCommand) void
    }
    class NotificationsContextFacadeImpl {
        -EmailCommandService emailCommandService
        +sendWelcomeEmail(emailAddress) void
        +sendVerificationCode(email, code) void
    }
    class PushNotificationHistoryQueryServiceImpl {
        -PushNotificationHistoryRepository pushNotificationHistoryRepository
        +handle(GetPushNotificationHistoryQuery) Page
    }
    class AlertIncidentChangedEventListener {
        -ExternalAlertingService externalAlertingService
        -ExternalDeviceService externalDeviceService
        -PushNotificationDeliveryService pushNotificationDeliveryService
        -PushNotificationHistoryRepository pushNotificationHistoryRepository
        +onAlertIncidentChanged(AlertIncidentChangedEvent) void
    }
    class ExternalDeviceService {
        <<interface>>
        +fetchOwnerUserIdByDeviceId(deviceId) Optional~UUID~
    }
    class ExternalAlertingService {
        <<interface>>
        +findAlertDetailsById(alertId) Optional~AlertDetails~
    }
    class SendVerificationCodeCommand {
        +EmailRecipient recipient
        +String verificationCode
    }
    class SendWelcomeEmailCommand {
        +EmailRecipient recipient
        +String name
    }
    class GetPushNotificationHistoryQuery {
        +UUID userId
        +Pageable pageable
    }
}

namespace domain {
    class AlertIncidentChangedEvent
    class EmailLog {
        -UUID id
        -EmailRecipient recipient
        -EmailSubject subject
        -EmailContent content
        -String status
        -Instant sentAt
        -String errorMessage
    }
    class PushNotificationLog {
        -UUID id
        -UUID userId
        -String title
        -String message
        -String status
        -Instant sentAt
        -String externalId
    }
    class EmailDeliveryService {
        <<interface>>
        +send(recipient, subject, content) void
    }
    class PushNotificationDeliveryService {
        <<interface>>
        +sendPush(userId, title, message) String
    }
    class EmailRecipient {
        +String value
    }
    class EmailSubject {
        +String value
    }
    class EmailContent {
        +String value
    }
    class EmailLogRepository {
        <<interface>>
        +save(emailLog) EmailLog
    }
    class PushNotificationLogRepository {
        <<interface>>
        +save(pushNotificationLog) PushNotificationLog
    }
    class EmailLogPersistence {
        <<interface>>
        +save(emailLog) EmailLog
    }
    class PushNotificationHistoryRepository {
        <<interface>>
        +save(pushNotificationLog) PushNotificationLog
        +findByUserId(userId, pageable) Page
    }
}

namespace infrastructure {
    class JpaEmailLogRepository {
        <<interface>>
    }
    class JpaPushNotificationLogRepository {
        <<interface>>
    }
    class SmtpEmailService {
        -JavaMailSender mailSender
        +send(recipient, subject, content) void
    }
    class OneSignalPushNotificationService {
        -String oneSignalAppId
        -String oneSignalApiKey
        +sendPush(userId, title, message) String
    }
}

AlertIncidentChangedEventListener --> PushNotificationDeliveryService : uses
AlertIncidentChangedEventListener --> AlertIncidentChangedEvent : @EventListener
AlertIncidentChangedEventListener --> PushNotificationHistoryRepository : uses
AlertIncidentChangedEventListener --> ExternalDeviceService : uses
AlertIncidentChangedEventListener --> ExternalAlertingService : uses
NotificationController --> PushNotificationHistoryQueryServiceImpl : uses

EmailCommandServiceImpl --> EmailDeliveryService : uses
EmailCommandServiceImpl --> EmailLogPersistence : uses

NotificationsContextFacadeImpl ..|> NotificationsContextFacade : implements
NotificationsContextFacadeImpl --> EmailCommandServiceImpl : uses
PushNotificationHistoryQueryServiceImpl --> PushNotificationHistoryRepository : uses

EmailLog --> EmailRecipient : contains
EmailLog --> EmailSubject : contains
EmailLog --> EmailContent : contains

SmtpEmailService ..|> EmailDeliveryService : implements
OneSignalPushNotificationService ..|> PushNotificationDeliveryService : implements
JpaEmailLogRepository ..|> EmailLogRepository : implements
JpaPushNotificationLogRepository ..|> PushNotificationLogRepository : implements
JpaEmailLogRepository ..|> EmailLogPersistence : implements
JpaPushNotificationLogRepository ..|> PushNotificationHistoryRepository : implements
```

---

## Layered Diagrams

### 1. Interfaces Layer

```mermaid
classDiagram
class NotificationController {
    +getUserNotifications(page) ResponseEntity
}
class NotificationsContextFacade {
    <<interface>>
    +sendWelcomeEmail(emailAddress) void
    +sendVerificationCode(email, code) void
}
class PushNotificationResponse {
    +UUID id
    +UUID userId
    +UUID alertId
    +String title
    +String message
    +String status
    +String errorMessage
    +Instant createdAt
}
```

### 2. Application Layer

```mermaid
classDiagram
class EmailCommandServiceImpl {
    -EmailDeliveryService emailDeliveryService
    -EmailLogPersistence emailLogPersistence
    +handle(SendVerificationCodeCommand) void
    +handle(SendWelcomeEmailCommand) void
}
class NotificationsContextFacadeImpl {
    -EmailCommandService emailCommandService
    +sendWelcomeEmail(emailAddress) void
    +sendVerificationCode(email, code) void
}
class PushNotificationHistoryQueryServiceImpl {
    -PushNotificationHistoryRepository pushNotificationHistoryRepository
    +handle(GetPushNotificationHistoryQuery) Page
}
class AlertIncidentChangedEventListener {
    -ExternalAlertingService externalAlertingService
    -ExternalDeviceService externalDeviceService
    -PushNotificationDeliveryService pushNotificationDeliveryService
    -PushNotificationHistoryRepository pushNotificationHistoryRepository
    +onAlertIncidentChanged(AlertIncidentChangedEvent) void
}
class ExternalDeviceService {
    <<interface>>
    +fetchOwnerUserIdByDeviceId(deviceId) Optional~UUID~
}
class ExternalAlertingService {
    <<interface>>
    +findAlertDetailsById(alertId) Optional~AlertDetails~
}
class SendVerificationCodeCommand {
    +EmailRecipient recipient
    +String verificationCode
}
class SendWelcomeEmailCommand {
    +EmailRecipient recipient
    +String name
}
class GetPushNotificationHistoryQuery {
    +UUID userId
    +Pageable pageable
}

NotificationsContextFacadeImpl --> EmailCommandServiceImpl : uses
AlertIncidentChangedEventListener --> ExternalDeviceService : uses
AlertIncidentChangedEventListener --> ExternalAlertingService : uses
PushNotificationHistoryQueryServiceImpl --> PushNotificationHistoryRepository : uses
NotificationController --> PushNotificationHistoryQueryServiceImpl : uses
```

### 3. Domain Layer

```mermaid
classDiagram
class EmailLog {
    -UUID id
    -EmailRecipient recipient
    -EmailSubject subject
    -EmailContent content
    -String status
    -Instant sentAt
    -String errorMessage
}
class PushNotificationLog {
    -UUID id
    -UUID userId
    -String title
    -String message
    -String status
    -Instant sentAt
    -String externalId
}
class EmailDeliveryService {
    <<interface>>
    +send(recipient, subject, content) void
}
class PushNotificationDeliveryService {
    <<interface>>
    +sendPush(userId, title, message) String
}
class EmailRecipient {
    +String value
}
class EmailSubject {
    +String value
}
class EmailContent {
    +String value
}
class EmailLogRepository {
    <<interface>>
    +save(emailLog) EmailLog
}
class PushNotificationLogRepository {
    <<interface>>
    +save(pushNotificationLog) PushNotificationLog
}
class EmailLogPersistence {
    <<interface>>
    +save(emailLog) EmailLog
}
class PushNotificationHistoryRepository {
    <<interface>>
    +findByUserId(userId, pageable) Page
}

EmailLog --> EmailRecipient : contains
EmailLog --> EmailSubject : contains
EmailLog --> EmailContent : contains
```

### 4. Infrastructure Layer

```mermaid
classDiagram
class JpaEmailLogRepository {
    <<interface>>
}
class JpaPushNotificationLogRepository {
    <<interface>>
}
class SmtpEmailService {
    -JavaMailSender mailSender
    +send(recipient, subject, content) void
}
class OneSignalPushNotificationService {
    -String oneSignalAppId
    -String oneSignalApiKey
    +sendPush(userId, title, message) String
}

JpaEmailLogRepository ..|> EmailLogPersistence : implements
JpaPushNotificationLogRepository ..|> PushNotificationHistoryRepository : implements
```
