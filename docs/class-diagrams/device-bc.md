# Device Bounded Context Class Diagrams

This document contains both the unified Bounded Context class diagram and the layered individual diagrams.

## Unified Class Diagram

```mermaid
---
title: DDD Class Diagram - Device Bounded Context (4 Layers)
---

classDiagram

namespace interfaces {
    class DeviceController {
        -DeviceCommandService deviceCommandService
        -DeviceQueryService deviceQueryService
        -DeviceStatusQueryService deviceStatusQueryService
        +pairDevice(request)
        +claimDevice(request)
        +getDevices(spaceId, page, size)
        +getDevice(deviceId)
        +getDeviceStatus(deviceId)
        +deleteDevice(deviceId)
        +updateDeviceName(deviceId, request)
    }
    class DeviceCommandController {
        -DeviceControlCommandService deviceControlCommandService
        -DeviceCommandQueryService deviceCommandQueryService
        +createDeviceCommand(deviceId, request)
        +getDeviceCommand(commandId)
    }
    class DeviceThresholdController {
        -DeviceThresholdCommandService thresholdCommandService
        -DeviceThresholdQueryService thresholdQueryService
        +writeThreshold(deviceId, request)
        +removeThreshold(deviceId, metric)
        +getThresholds(deviceId)
    }
    class OrganizationController {
        -OrganizationCommandService organizationCommandService
        +createOrganization(request)
        +updateOrganizationName(id, request)
        +deleteOrganization(id)
    }
    class SpaceController {
        -SpaceCommandService spaceCommandService
        +createSpace(request)
        +updateSpaceName(id, request)
        +deleteSpace(id)
    }
}

namespace application {
    class DeviceCommandServiceImpl {
        -DeviceRepository deviceRepository
        -DeviceAssignmentRepository deviceAssignmentRepository
        -SpaceRepository spaceRepository
        -ProvisioningDevicesChangedPublisher provisioningDevicesChangedPublisher
        +handle(PairDeviceCommand) DeviceAssignment
        +handle(ClaimDeviceCommand) DeviceAssignment
        +handle(ResetDeviceAssignmentCommand)
        +handle(UpdateDeviceNameCommand)
        +handle(SeedDevicesCommand) List~Device~
    }
    class DeviceControlCommandServiceImpl {
        -DeviceCommandRepository deviceCommandRepository
        -DeviceAssignmentRepository deviceAssignmentRepository
        -DeviceCommandsPendingPublisher deviceCommandsPendingPublisher
        +handle(CreateDeviceCommandCommand) DeviceCommand
        +handle(DispatchPendingDeviceCommandsCommand)
    }
    class DevicePresenceCommandServiceImpl {
        -DeviceAssignmentRepository deviceAssignmentRepository
        +handle(UpdateDevicePresenceStatusCommand)
    }
    class DeviceThresholdCommandServiceImpl {
        -DeviceAssignmentRepository deviceAssignmentRepository
        +handle(WriteDeviceThresholdCommand)
        +handle(RemoveDeviceThresholdCommand)
    }
    class OrganizationCommandServiceImpl {
        -OrganizationRepository organizationRepository
        +handle(CreateOrganizationCommand) Organization
        +handle(UpdateOrganizationNameCommand)
        +handle(DeleteOrganizationCommand)
    }
    class SpaceCommandServiceImpl {
        -SpaceRepository spaceRepository
        -OrganizationRepository organizationRepository
        -DeviceAssignmentRepository deviceAssignmentRepository
        -ExternalBillingService externalBillingService
        +handle(CreateSpaceCommand) Space
        +handle(UpdateSpaceNameCommand)
        +handle(DeleteSpaceCommand)
    }
}

namespace domain {
    class Device {
        -UUID id
        -String serialNumber
        -String name
        -String factoryName
        -HardwareId hardwareId
        -ApiKey apiKey
        -DeviceType deviceType
        +rotateApiKey(apiKey)
        +updateName(name)
        +resetNameToFactoryDefault()
    }
    class DeviceAssignment {
        -UUID id
        -Device device
        -UserId ownerUserId
        -UUID spaceId
        -DeviceStatus status
        -ClaimToken claimToken
        -Instant activatedAt
        -Instant lastSeenAt
        +claimToSpace(spaceId, userId)
        +markOnline()
        +markOffline()
        +updatePresence(status, occurredAt)
    }
    class DeviceCommand {
        -UUID id
        -UUID deviceId
        -String commandType
        -String payload
        -String status
        -Instant createdAt
    }
    class Organization {
        -UUID id
        -String name
        -UserId ownerUserId
        +updateName(name)
    }
    class Space {
        -UUID id
        -String name
        -UUID organizationId
        -UserId ownerUserId
        +updateName(name)
    }
    class DeviceRepository {
        <<interface>>
        +save(device) Device
        +findById(id) Optional~Device~
        +findByHardwareId(hardwareId) Optional~Device~
    }
    class DeviceAssignmentRepository {
        <<interface>>
        +save(assignment) DeviceAssignment
        +findByDeviceId(deviceId) Optional~DeviceAssignment~
        +findByClaimToken(claimToken) Optional~DeviceAssignment~
        +findBySpaceId(spaceId, pageable) Page~DeviceAssignment~
        +existsBySpaceId(spaceId) boolean
    }
    class DeviceCommandRepository {
        <<interface>>
        +save(command) DeviceCommand
        +findById(id) Optional~DeviceCommand~
    }
    class OrganizationRepository {
        <<interface>>
        +save(organization) Organization
        +findById(id) Optional~Organization~
    }
    class SpaceRepository {
        <<interface>>
        +save(space) Space
        +findById(id) Optional~Space~
        +countByOwnerUserId(ownerUserId) int
    }
}

namespace infrastructure {
    class JpaDeviceRepository {
        <<interface>>
        +findByHardwareId(hardwareId)
        +findBySerialNumber(serialNumber)
    }
    class JpaDeviceAssignmentRepository {
        <<interface>>
        +findByDeviceId(deviceId)
        +findByClaimToken(claimToken)
    }
    class JpaDeviceCommandRepository {
        <<interface>>
    }
    class JpaOrganizationRepository {
        <<interface>>
    }
    class JpaSpaceRepository {
        <<interface>>
    }
    class ProvisioningDevicesChangedPublisher {
        -EdgeEventPublisher edgeEventPublisher
        +publish(DeviceChangedIntegrationEvent) void
    }
    class DeviceCommandsPendingPublisher {
        -EdgeEventPublisher edgeEventPublisher
        +publish(DeviceCommandIssuedIntegrationEvent) void
    }
}

DeviceController --> DeviceCommandServiceImpl : uses
DeviceCommandController --> DeviceControlCommandServiceImpl : uses
DeviceThresholdController --> DeviceThresholdCommandServiceImpl : uses
OrganizationController --> OrganizationCommandServiceImpl : uses
SpaceController --> SpaceCommandServiceImpl : uses

DeviceCommandServiceImpl --> DeviceRepository : uses
DeviceCommandServiceImpl --> DeviceAssignmentRepository : uses
DeviceCommandServiceImpl --> SpaceRepository : uses

DeviceControlCommandServiceImpl --> DeviceCommandRepository : uses
DeviceControlCommandServiceImpl --> DeviceAssignmentRepository : uses
ProvisioningDevicesChangedPublisher --> EdgeEventPublisher : notifies edge via HTTP
DeviceCommandsPendingPublisher --> EdgeEventPublisher : notifies edge via HTTP

DeviceThresholdCommandServiceImpl --> DeviceAssignmentRepository : uses

OrganizationCommandServiceImpl --> OrganizationRepository : uses

SpaceCommandServiceImpl --> SpaceRepository : uses
SpaceCommandServiceImpl --> OrganizationRepository : uses
SpaceCommandServiceImpl --> DeviceAssignmentRepository : uses

DeviceAssignment --> Device : contains
Space --> Organization : belongs to organizationId

JpaDeviceRepository ..|> DeviceRepository : implements
JpaDeviceAssignmentRepository ..|> DeviceAssignmentRepository : implements
JpaDeviceCommandRepository ..|> DeviceCommandRepository : implements
JpaOrganizationRepository ..|> OrganizationRepository : implements
JpaSpaceRepository ..|> SpaceRepository : implements
```

---

## Layered Diagrams

### 1. Interfaces Layer

```mermaid
classDiagram
class DeviceController {
    -DeviceCommandService deviceCommandService
    -DeviceQueryService deviceQueryService
    -DeviceStatusQueryService deviceStatusQueryService
    +pairDevice(request)
    +claimDevice(request)
    +getDevices(spaceId, page, size)
    +getDevice(deviceId)
    +getDeviceStatus(deviceId)
    +deleteDevice(deviceId)
    +updateDeviceName(deviceId, request)
}
class DeviceCommandController {
    -DeviceControlCommandService deviceControlCommandService
    -DeviceCommandQueryService deviceCommandQueryService
    +createDeviceCommand(deviceId, request)
    +getDeviceCommand(commandId)
}
class DeviceThresholdController {
    -DeviceThresholdCommandService thresholdCommandService
    -DeviceThresholdQueryService thresholdQueryService
    +writeThreshold(deviceId, request)
    +removeThreshold(deviceId, metric)
    +getThresholds(deviceId)
}
class OrganizationController {
    -OrganizationCommandService organizationCommandService
    +createOrganization(request)
    +updateOrganizationName(id, request)
    +deleteOrganization(id)
}
class SpaceController {
    -SpaceCommandService spaceCommandService
    +createSpace(request)
    +updateSpaceName(id, request)
    +deleteSpace(id)
}
```

### 2. Application Layer

```mermaid
classDiagram
class DeviceCommandServiceImpl {
    -DeviceRepository deviceRepository
    -DeviceAssignmentRepository deviceAssignmentRepository
    -SpaceRepository spaceRepository
    -ProvisioningDevicesChangedPublisher provisioningDevicesChangedPublisher
    +handle(PairDeviceCommand) DeviceAssignment
    +handle(ClaimDeviceCommand) DeviceAssignment
    +handle(ResetDeviceAssignmentCommand)
    +handle(UpdateDeviceNameCommand)
    +handle(SeedDevicesCommand) List~Device~
}
class DeviceControlCommandServiceImpl {
    -DeviceCommandRepository deviceCommandRepository
    -DeviceAssignmentRepository deviceAssignmentRepository
    -DeviceCommandsPendingPublisher deviceCommandsPendingPublisher
    +handle(CreateDeviceCommandCommand) DeviceCommand
    +handle(DispatchPendingDeviceCommandsCommand)
}
class DevicePresenceCommandServiceImpl {
    -DeviceAssignmentRepository deviceAssignmentRepository
    +handle(UpdateDevicePresenceStatusCommand)
}
class DeviceThresholdCommandServiceImpl {
    -DeviceAssignmentRepository deviceAssignmentRepository
    +handle(WriteDeviceThresholdCommand)
    +handle(RemoveDeviceThresholdCommand)
}
class OrganizationCommandServiceImpl {
    -OrganizationRepository organizationRepository
    +handle(CreateOrganizationCommand) Organization
    +handle(UpdateOrganizationNameCommand)
    +handle(DeleteOrganizationCommand)
}
class SpaceCommandServiceImpl {
    -SpaceRepository spaceRepository
    -OrganizationRepository organizationRepository
    -DeviceAssignmentRepository deviceAssignmentRepository
    -ExternalBillingService externalBillingService
    +handle(CreateSpaceCommand) Space
    +handle(UpdateSpaceNameCommand)
    +handle(DeleteSpaceCommand)
}
```

### 3. Domain Layer

```mermaid
classDiagram
class Device {
    -UUID id
    -String serialNumber
    -String name
    -String factoryName
    -HardwareId hardwareId
    -ApiKey apiKey
    -DeviceType deviceType
    +rotateApiKey(apiKey)
    +updateName(name)
    +resetNameToFactoryDefault()
}
class DeviceAssignment {
    -UUID id
    -Device device
    -UserId ownerUserId
    -UUID spaceId
    -DeviceStatus status
    -ClaimToken claimToken
    -Instant activatedAt
    -Instant lastSeenAt
    +claimToSpace(spaceId, userId)
    +markOnline()
    +markOffline()
    +updatePresence(status, occurredAt)
}
class DeviceCommand {
    -UUID id
    -UUID deviceId
    -String commandType
    -String payload
    -String status
    -Instant createdAt
}
class Organization {
    -UUID id
    -String name
    -UserId ownerUserId
    +updateName(name)
}
class Space {
    -UUID id
    -String name
    -UUID organizationId
    -UserId ownerUserId
    +updateName(name)
}
class DeviceRepository {
    <<interface>>
    +save(device) Device
    +findById(id) Optional~Device~
    +findByHardwareId(hardwareId) Optional~Device~
}
class DeviceAssignmentRepository {
    <<interface>>
    +save(assignment) DeviceAssignment
    +findByDeviceId(deviceId) Optional~DeviceAssignment~
    +findByClaimToken(claimToken) Optional~DeviceAssignment~
    +findBySpaceId(spaceId, pageable) Page~DeviceAssignment~
    +existsBySpaceId(spaceId) boolean
}
class DeviceCommandRepository {
    <<interface>>
    +save(command) DeviceCommand
    +findById(id) Optional~DeviceCommand~
}
class OrganizationRepository {
    <<interface>>
    +save(organization) Organization
    +findById(id) Optional~Organization~
}
class SpaceRepository {
    <<interface>>
    +save(space) Space
    +findById(id) Optional~Space~
    +countByOwnerUserId(ownerUserId) int
}

DeviceAssignment --> Device : contains
Space --> Organization : belongs to organizationId
```

### 4. Infrastructure Layer

```mermaid
classDiagram
class JpaDeviceRepository {
    <<interface>>
    +findByHardwareId(hardwareId)
    +findBySerialNumber(serialNumber)
}
class JpaDeviceAssignmentRepository {
    <<interface>>
    +findByClaimToken(claimToken)
    +findByDeviceId(deviceId)
}
class JpaDeviceCommandRepository {
    <<interface>>
}
class JpaOrganizationRepository {
    <<interface>>
}
class JpaSpaceRepository {
    <<interface>>
}
class ProvisioningDevicesChangedPublisher {
    -EdgeEventPublisher edgeEventPublisher
    +publish(DeviceChangedIntegrationEvent) void
}
class DeviceCommandsPendingPublisher {
    -EdgeEventPublisher edgeEventPublisher
    +publish(DeviceCommandIssuedIntegrationEvent) void
}
```
