package com.claircore.alerting.application.internal.outboundservices.acl;

import com.claircore.device.interfaces.acl.DeviceContextFacade;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ExternalAlertingDeviceService {

    private final DeviceContextFacade deviceContextFacade;

    public ExternalAlertingDeviceService(DeviceContextFacade deviceContextFacade) {
        this.deviceContextFacade = deviceContextFacade;
    }

    /**
     * Spring cache serialization can deserialize UUIDs as Strings depending on the cache serializer.
     * Cache the UUID as a String to avoid ClassCastException at call sites expecting UUID.
     */
    public Optional<UUID> fetchSpaceIdByDeviceId(UUID deviceId) {
        return Optional.ofNullable(fetchSpaceIdStringByDeviceIdCached(deviceId))
                .flatMap(ExternalAlertingDeviceService::parseUuid);
    }

    @Cacheable(value = "alerting:device-space-id", key = "#deviceId")
    public String fetchSpaceIdStringByDeviceIdCached(UUID deviceId) {
        return deviceContextFacade.findSpaceIdByDeviceId(deviceId)
                .map(UUID::toString)
                .orElse(null);
    }

    private static Optional<UUID> parseUuid(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public Optional<UUID> fetchDeviceIdByHardwareId(String hardwareId) {
        return deviceContextFacade.findDeviceIdByHardwareId(hardwareId);
    }

    public Map<UUID, String> fetchDeviceNamesByDeviceIds(List<UUID> deviceIds) {
        return deviceContextFacade.findDeviceNamesByDeviceIds(deviceIds);
    }

    public Map<UUID, String> fetchSpaceNamesBySpaceIds(List<UUID> spaceIds) {
        return deviceContextFacade.findSpaceNamesBySpaceIds(spaceIds);
    }

    /** Batch lookup, so a page of alerts costs one call instead of one per alert. */
    public Map<UUID, String> fetchHardwareIdsByDeviceIds(List<UUID> deviceIds) {
        return deviceContextFacade.findHardwareIdsByDeviceIds(deviceIds);
    }

    @Cacheable(value = "alerting:device-hardware", key = "#deviceId")
    public Optional<String> fetchHardwareIdByDeviceId(UUID deviceId) {
        return deviceContextFacade.findHardwareIdByDeviceId(deviceId);
    }

    public boolean verifyDeviceOwnership(UUID deviceId, UUID userId) {
        return deviceContextFacade.isDeviceOwnedByUser(deviceId, userId);
    }

    public boolean verifySpaceOwnership(UUID spaceId, UUID userId) {
        return deviceContextFacade.isSpaceOwnedByUser(spaceId, userId);
    }

    public List<UUID> fetchDeviceIdsByOwnerId(UUID ownerUserId) {
        return deviceContextFacade.findDeviceIdsByOwnerId(ownerUserId);
    }

    @Cacheable(value = "alerting:space-name", key = "#spaceId")
    public Optional<String> fetchSpaceNameBySpaceId(UUID spaceId) {
        return deviceContextFacade.findSpaceNameBySpaceId(spaceId);
    }

    @Cacheable(value = "alerting:device-name", key = "#deviceId")
    public Optional<String> fetchDeviceNameByDeviceId(UUID deviceId) {
        return deviceContextFacade.findDeviceNameByDeviceId(deviceId);
    }
}
