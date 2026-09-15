# Billing Bounded Context Class Diagrams

This document contains both the unified Bounded Context class diagram and the layered individual diagrams.

## Unified Class Diagram

```mermaid
---
title: DDD Class Diagram - Billing Bounded Context (4 Layers)
---

classDiagram

namespace interfaces {
    class SubscriptionController {
        -SubscriptionCommandService subscriptionCommandService
        -SubscriptionQueryService subscriptionQueryService
        +createCheckoutSession(resource) ResponseEntity
        +createPaymentIntent(resource) ResponseEntity
        +getSubscriptionsByUserId(userId) ResponseEntity
        +getUserPlan(userId) ResponseEntity
        +downgradeToFreemium(userId) ResponseEntity
    }
    class StripeWebhookController {
        -SubscriptionCommandService subscriptionCommandService
        -String stripeWebhookSecret
        +handleWebhook(payload, sigHeader) ResponseEntity
    }
    class StaticWebController {
        +checkout() String
        +success() String
        +cancel() String
    }
    class BillingContextFacade {
        <<interface>>
        +getMaxOrganizations(userId) int
        +getMaxSpaces(userId) int
        +getMaxDevices(userId) int
    }
}

namespace application {
    class SubscriptionCommandServiceImpl {
        -PaymentGateway paymentGateway
        -PaymentRecordRepository paymentRecordRepository
        -UserPlanRepository userPlanRepository
        +handle(CreateCheckoutSessionCommand) String
        +handle(CreatePaymentIntentCommand) String
        +handle(FulfillSubscriptionCommand) void
        +handle(DowngradeToFreemiumCommand) void
    }
    class SubscriptionQueryServiceImpl {
        -UserPlanRepository userPlanRepository
        -PaymentRecordRepository paymentRecordRepository
        +handle(GetSubscriptionByIdQuery) Optional~PaymentRecord~
        +handle(GetSubscriptionsByUserIdQuery) List~PaymentRecord~
        +resolveUserPlan(GetUserPlanQuery) String
    }
    class BillingContextFacadeImpl {
        -UserPlanRepository userPlanRepository
        -resolveUserPlanType(userId) PlanType
        +getMaxOrganizations(userId) int
        +getMaxSpaces(userId) int
        +getMaxDevices(userId) int
    }
    class SubscriptionPaidEventHandler {
        -SubscriptionCommandService subscriptionCommandService
        +handle(SubscriptionPaidEvent) void
    }
    class UserRegisteredEventHandler {
        -UserPlanRepository userPlanRepository
        +handle(UserRegisteredEvent) void
    }
}

namespace domain {
    class UserPlan {
        -UUID id
        -UserId userId
        -PlanType planType
        -LocalDate startDate
        -LocalDate endDate
        +upgradeToPremium() void
        +downgradeToFreemium() void
        +isPremiumExpired() boolean
    }
    class PaymentRecord {
        -UUID id
        -UserId userId
        -Money amount
        -PaymentStatus status
        -String stripePaymentIntentId
        +markAsCompleted() void
    }
    class PaymentGateway {
        <<interface>>
        +createCheckoutSession(userId, amount) String
        +createPaymentIntent(userId, amount) String
    }
    class PlanType {
        <<enumeration>>
        FREEMIUM
        PREMIUM
    }
    class PaymentStatus {
        <<enumeration>>
        PENDING
        COMPLETED
        FAILED
    }
    class Money {
        +BigDecimal amount
        +String currency
    }
    class UserId {
        +UUID userId
    }
    class UserPlanRepository {
        <<interface>>
        +save(userPlan) UserPlan
        +findByUserId(userId) Optional~UserPlan~
    }
    class PaymentRecordRepository {
        <<interface>>
        +save(paymentRecord) PaymentRecord
        +findByStripePaymentIntentId(id) Optional~PaymentRecord~
        +findByUserId(userId) List~PaymentRecord~
    }
}

namespace infrastructure {
    class StripePaymentGatewayAdapter {
        -String stripeApiKey
        +createCheckoutSession(userId, amount) String
        +createPaymentIntent(userId, amount) String
    }
    class JpaUserPlanRepository {
        <<interface>>
    }
    class JpaPaymentRecordRepository {
        <<interface>>
    }
}

SubscriptionController --> SubscriptionCommandServiceImpl : uses
SubscriptionController --> SubscriptionQueryServiceImpl : uses
StripeWebhookController --> SubscriptionCommandServiceImpl : uses

SubscriptionCommandServiceImpl --> PaymentGateway : uses
SubscriptionCommandServiceImpl --> PaymentRecordRepository : uses
SubscriptionCommandServiceImpl --> UserPlanRepository : uses

SubscriptionQueryServiceImpl --> UserPlanRepository : uses
SubscriptionQueryServiceImpl --> PaymentRecordRepository : uses

BillingContextFacadeImpl ..|> BillingContextFacade : implements
BillingContextFacadeImpl --> UserPlanRepository : uses

StripePaymentGatewayAdapter ..|> PaymentGateway : implements

UserPlan --> UserId : contains
UserPlan --> PlanType : contains

PaymentRecord --> UserId : contains
PaymentRecord --> Money : contains
PaymentRecord --> PaymentStatus : contains

JpaUserPlanRepository ..|> UserPlanRepository : implements
JpaPaymentRecordRepository ..|> PaymentRecordRepository : implements
```

---

## Layered Diagrams

### 1. Interfaces Layer

```mermaid
classDiagram
class SubscriptionController {
    -SubscriptionCommandService subscriptionCommandService
    -SubscriptionQueryService subscriptionQueryService
    +createCheckoutSession(resource) ResponseEntity
    +createPaymentIntent(resource) ResponseEntity
    +getSubscriptionsByUserId(userId) ResponseEntity
    +getUserPlan(userId) ResponseEntity
    +downgradeToFreemium(userId) ResponseEntity
}
class StripeWebhookController {
    -SubscriptionCommandService subscriptionCommandService
    -String stripeWebhookSecret
    +handleWebhook(payload, sigHeader) ResponseEntity
}
class StaticWebController {
    +checkout() String
    +success() String
    +cancel() String
}
class BillingContextFacade {
    <<interface>>
    +getMaxOrganizations(userId) int
    +getMaxSpaces(userId) int
    +getMaxDevices(userId) int
}
```

### 2. Application Layer

```mermaid
classDiagram
class SubscriptionCommandServiceImpl {
    -PaymentGateway paymentGateway
    -PaymentRecordRepository paymentRecordRepository
    -UserPlanRepository userPlanRepository
    +handle(CreateCheckoutSessionCommand) String
    +handle(CreatePaymentIntentCommand) String
    +handle(FulfillSubscriptionCommand) void
    +handle(DowngradeToFreemiumCommand) void
}
class SubscriptionQueryServiceImpl {
    -UserPlanRepository userPlanRepository
    -PaymentRecordRepository paymentRecordRepository
    +handle(GetSubscriptionByIdQuery) Optional~PaymentRecord~
    +handle(GetSubscriptionsByUserIdQuery) List~PaymentRecord~
    +resolveUserPlan(GetUserPlanQuery) String
}
class BillingContextFacadeImpl {
    -UserPlanRepository userPlanRepository
    -resolveUserPlanType(userId) PlanType
    +getMaxOrganizations(userId) int
    +getMaxSpaces(userId) int
    +getMaxDevices(userId) int
}
class SubscriptionPaidEventHandler {
    -SubscriptionCommandService subscriptionCommandService
    +handle(SubscriptionPaidEvent) void
}
class UserRegisteredEventHandler {
    -UserPlanRepository userPlanRepository
    +handle(UserRegisteredEvent) void
}
```

### 3. Domain Layer

```mermaid
classDiagram
class UserPlan {
    -UUID id
    -UserId userId
    -PlanType planType
    -LocalDate startDate
    -LocalDate endDate
    +upgradeToPremium() void
    +downgradeToFreemium() void
    +isPremiumExpired() boolean
}
class PaymentRecord {
    -UUID id
    -UserId userId
    -Money amount
    -PaymentStatus status
    -String stripePaymentIntentId
    +markAsCompleted() void
}
class PaymentGateway {
    <<interface>>
    +createCheckoutSession(userId, amount) String
    +createPaymentIntent(userId, amount) String
}
class PlanType {
    <<enumeration>>
    FREEMIUM
    PREMIUM
}
class PaymentStatus {
    <<enumeration>>
    PENDING
    COMPLETED
    FAILED
}
class Money {
    +BigDecimal amount
    +String currency
}
class UserId {
    +UUID userId
}
class UserPlanRepository {
    <<interface>>
    +save(userPlan) UserPlan
    +findByUserId(userId) Optional~UserPlan~
}
class PaymentRecordRepository {
    <<interface>>
    +save(paymentRecord) PaymentRecord
    +findByStripePaymentIntentId(id) Optional~PaymentRecord~
    +findByUserId(userId) List~PaymentRecord~
}

UserPlan --> UserId : contains
UserPlan --> PlanType : contains
PaymentRecord --> UserId : contains
PaymentRecord --> Money : contains
PaymentRecord --> PaymentStatus : contains
```

### 4. Infrastructure Layer

```mermaid
classDiagram
class StripePaymentGatewayAdapter {
    -String stripeApiKey
    +createCheckoutSession(userId, amount) String
    +createPaymentIntent(userId, amount) String
}
class JpaUserPlanRepository {
    <<interface>>
}
class JpaPaymentRecordRepository {
    <<interface>>
}
```
