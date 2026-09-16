package com.claircore.device.domain.model.aggregates;

import com.claircore.device.domain.model.valueobjects.ClaimToken;
import com.claircore.device.domain.model.valueobjects.DeviceStatus;
import com.claircore.device.domain.model.valueobjects.DeviceType;
import com.claircore.device.domain.model.valueobjects.HardwareId;
import com.claircore.device.domain.model.valueobjects.ApiKey;
import com.claircore.device.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class DeviceAssignmentTest {

    @Test
    void shouldStartOfflineWhenAssignmentIsCreated() {
        DeviceAssignment assignment = new DeviceAssignment(device().getId(), ClaimToken.generate());

        assertEquals(DeviceStatus.OFFLINE, assignment.getStatus());
    }

    @Test
    void shouldClaimToSpaceWhenAssignmentIsUnclaimed() {
        DeviceAssignment assignment = new DeviceAssignment(device().getId(), ClaimToken.generate());
        UUID spaceId = UUID.fromString("550e8400-e29b-41d4-a716-446655440500");
        UserId userId = new UserId(UUID.fromString("550e8400-e29b-41d4-a716-446655440501"));

        assignment.claimToSpace(spaceId, userId);

        assertEquals(spaceId, assignment.getSpaceId());
        assertEquals(userId, assignment.getOwnerUserId());
        assertEquals(null, assignment.getClaimToken());
        assertNotNull(assignment.getActivatedAt());
    }

    @Test
    void shouldRejectClaimWhenAssignmentWasAlreadyClaimed() {
        DeviceAssignment assignment = new DeviceAssignment(device().getId(), ClaimToken.generate());
        assignment.claimToSpace(UUID.randomUUID(), new UserId(UUID.randomUUID()));

        IllegalStateException exception = assertThrowsExactly(
                IllegalStateException.class,
                () -> assignment.claimToSpace(UUID.randomUUID(), new UserId(UUID.randomUUID()))
        );

        assertEquals("Device already claimed", exception.getMessage());
    }

    @Test
    void shouldUpdatePresenceWhenStatusIsOnline() {
        DeviceAssignment assignment = new DeviceAssignment(device().getId(), ClaimToken.generate());

        assignment.updatePresence(DeviceStatus.ONLINE, Instant.parse("2026-06-05T12:45:00Z"));

        assertEquals(DeviceStatus.ONLINE, assignment.getStatus());
        assertNotNull(assignment.getLastSeenAt());
    }

    @Test
    void presenceEventsApplyInOccurrenceOrderAndNeverRegress() {
        DeviceAssignment assignment = new DeviceAssignment(device().getId(), ClaimToken.generate());
        Instant t0 = Instant.parse("2026-06-05T12:00:00Z");
        Instant t1 = t0.plusSeconds(60);
        Instant t2 = t0.plusSeconds(120);
        assertEquals(true, assignment.updatePresence(DeviceStatus.ONLINE, t2));
        assertEquals(false, assignment.updatePresence(DeviceStatus.OFFLINE, t1));
        assertEquals(false, assignment.updatePresence(DeviceStatus.ONLINE, t0));
        assertEquals(DeviceStatus.ONLINE, assignment.getStatus());
        assertEquals(t2, assignment.getLastSeenAt());
        assertEquals(t2, assignment.getPresenceAt());
        // Exact duplicate is ignored; a newer OFFLINE applies and keeps lastSeenAt.
        assertEquals(false, assignment.updatePresence(DeviceStatus.ONLINE, t2));
        assertEquals(true, assignment.updatePresence(DeviceStatus.OFFLINE, t2.plusSeconds(1)));
        assertEquals(DeviceStatus.OFFLINE, assignment.getStatus());
        assertEquals(t2, assignment.getLastSeenAt());
    }

    @Test
    void connectivityNewsRefreshesLastSeenButDoesNotLeaveStandby() {
        DeviceAssignment assignment = new DeviceAssignment(device().getId(), ClaimToken.generate());
        assignment.markStandby();
        Instant later = Instant.now().plusSeconds(1);
        assertEquals(true, assignment.updatePresence(DeviceStatus.ONLINE, later));
        assertEquals(DeviceStatus.STANDBY, assignment.getStatus());
        assertEquals(later, assignment.getLastSeenAt());
        assignment.markOnline();
        assertEquals(DeviceStatus.ONLINE, assignment.getStatus());
    }

    @Test
    void aPresenceEventFarInTheFutureIsRejected() {
        DeviceAssignment assignment = new DeviceAssignment(device().getId(), ClaimToken.generate());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> assignment.updatePresence(DeviceStatus.ONLINE, Instant.now().plusSeconds(3600)));
    }

    private Device device() {
        return new Device(
                "SN-0002",
                "Sensor 0002",
                new HardwareId("CLAIR-0KBG"),
                ApiKey.generate(),
                new DeviceType("air-quality-v1")
        );
    }
}
