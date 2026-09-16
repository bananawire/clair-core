package com.claircore.analytics.application.internal.outboundservices.acl;

import com.claircore.device.interfaces.acl.DeviceContextFacade;
import com.claircore.device.interfaces.acl.OrganizationSummary;
import com.claircore.device.interfaces.acl.SpaceSummary;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Consumer-side ACL for resolving Device identifiers from the Device bounded context.
 */
@Service("analyticsExternalDeviceService")
public class ExternalDeviceService {

    private final DeviceContextFacade deviceContextFacade;

    public ExternalDeviceService(DeviceContextFacade deviceContextFacade) {
        this.deviceContextFacade = deviceContextFacade;
    }

    public boolean isDeviceOwnedByUser(UUID deviceId, UUID userId) {
        return deviceContextFacade.isDeviceOwnedByUser(deviceId, userId);
    }

    public List<OrganizationSummary> findOrganizationsByOwnerId(UUID ownerUserId) {
        if (ownerUserId == null) return List.of();
        return deviceContextFacade.findOrganizationsByOwnerId(ownerUserId);
    }

    public List<SpaceSummary> findSpacesByOrganizationId(UUID organizationId) {
        if (organizationId == null) return List.of();
        return deviceContextFacade.findSpacesByOrganizationId(organizationId);
    }

    public List<UUID> findDeviceIdsBySpaceId(UUID spaceId, int limit) {
        if (spaceId == null) return List.of();
        return deviceContextFacade.findDeviceIdsBySpaceId(spaceId, limit);
    }

    public Map<UUID, String> findDeviceNamesByDeviceIds(List<UUID> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) return Collections.emptyMap();
        return deviceContextFacade.findDeviceNamesByDeviceIds(deviceIds);
    }

    public Map<UUID, String> findSpaceNamesBySpaceIds(List<UUID> spaceIds) {
        if (spaceIds == null || spaceIds.isEmpty()) return Collections.emptyMap();
        return deviceContextFacade.findSpaceNamesBySpaceIds(spaceIds);
    }
}
