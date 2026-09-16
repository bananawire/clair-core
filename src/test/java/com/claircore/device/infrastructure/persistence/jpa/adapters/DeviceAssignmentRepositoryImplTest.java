package com.claircore.device.infrastructure.persistence.jpa.adapters;

import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.valueobjects.ClaimToken;
import com.claircore.device.domain.model.valueobjects.DeviceStatus;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.domain.repositories.DeviceAssignmentRepository;
import com.claircore.shared.infrastructure.config.JpaAuditingConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JpaAuditingConfiguration.class, DeviceAssignmentRepositoryImpl.class})
class DeviceAssignmentRepositoryImplTest {

    @Autowired
    private DeviceAssignmentRepository repository;

    @Test
    void savesWithTheIdentityTheAggregateAssignedAndFillsTheAuditTimestamps() {
        var assignment = new DeviceAssignment(UUID.randomUUID(), ClaimToken.generate());

        var saved = repository.save(assignment);

        assertThat(saved.getId()).isEqualTo(assignment.getId());
        assertThat(saved.getStatus()).isEqualTo(DeviceStatus.OFFLINE);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    /**
     * The configuration map is where every configured threshold lives, and nothing else reads the
     * column — an assembler that dropped it would lose them with no other test failing.
     */
    @Test
    void roundTripsAConfigurationMapWithSeveralEntries() {
        var assignment = new DeviceAssignment(UUID.randomUUID(), ClaimToken.generate());
        assignment.putConfigurationValue("threshold.CO2", "{\"metric\":\"CO2\",\"value\":1000,\"enabled\":true}");
        assignment.putConfigurationValue("threshold.PM2_5", "{\"metric\":\"PM2_5\",\"value\":35,\"enabled\":false}");
        assignment.putConfigurationValue("reporting.intervalSeconds", "60");

        repository.save(assignment);
        var found = repository.findById(assignment.getId()).orElseThrow();

        assertThat(found.getConfiguration()).hasSize(3).containsAllEntriesOf(Map.of(
                "threshold.CO2", "{\"metric\":\"CO2\",\"value\":1000,\"enabled\":true}",
                "threshold.PM2_5", "{\"metric\":\"PM2_5\",\"value\":35,\"enabled\":false}",
                "reporting.intervalSeconds", "60"));
    }

    @Test
    void removingAConfigurationEntryRemovesTheStoredRow() {
        var assignment = new DeviceAssignment(UUID.randomUUID(), ClaimToken.generate());
        assignment.putConfigurationValue("threshold.CO2", "{}");
        assignment.putConfigurationValue("threshold.PM2_5", "{}");
        repository.save(assignment);

        var stored = repository.findById(assignment.getId()).orElseThrow();
        stored.removeConfigurationValue("threshold.CO2");
        repository.save(stored);

        assertThat(repository.findById(assignment.getId()).orElseThrow().getConfiguration())
                .containsOnlyKeys("threshold.PM2_5");
    }

    @Test
    void roundTripsEveryOtherFieldThroughStorage() {
        UUID deviceId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        var assignment = new DeviceAssignment(deviceId, ClaimToken.generate());
        assignment.claimToSpace(spaceId, new UserId(ownerId));
        assignment.markOnline();
        repository.save(assignment);

        var found = repository.findByDeviceId(deviceId).orElseThrow();

        assertThat(found.getDeviceId()).isEqualTo(deviceId);
        assertThat(found.getSpaceId()).isEqualTo(spaceId);
        assertThat(found.getOwnerUserId()).isEqualTo(new UserId(ownerId));
        assertThat(found.getStatus()).isEqualTo(DeviceStatus.ONLINE);
        assertThat(found.getActivatedAt()).isNotNull();
        assertThat(found.getLastSeenAt()).isNotNull();
        // Claiming clears the token; it is single-use.
        assertThat(found.getClaimToken()).isNull();
    }

    @Test
    void findsAnUnclaimedAssignmentByItsClaimToken() {
        var token = ClaimToken.generate();
        var assignment = repository.save(new DeviceAssignment(UUID.randomUUID(), token));

        assertThat(repository.findByClaimToken(token.value()))
                .map(DeviceAssignment::getId).contains(assignment.getId());
    }

    @Test
    void countsAndOwnershipChecksAreScopedToTheOwner() {
        UUID deviceId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        var assignment = new DeviceAssignment(deviceId, ClaimToken.generate());
        assignment.claimToSpace(spaceId, new UserId(ownerId));
        repository.save(assignment);

        assertThat(repository.countBySpaceId(spaceId)).isEqualTo(1);
        assertThat(repository.existsBySpaceId(spaceId)).isTrue();
        assertThat(repository.existsByDeviceIdAndOwnerUserId(deviceId, new UserId(ownerId))).isTrue();
        assertThat(repository.existsByDeviceIdAndOwnerUserId(deviceId, new UserId(UUID.randomUUID()))).isFalse();
        assertThat(repository.findDeviceIdsByOwnerUserId(new UserId(ownerId))).containsExactly(deviceId);
    }

    @Test
    void updatingPresenceMovesTheTimestampButOfflineDoesNot() {
        var assignment = repository.save(new DeviceAssignment(UUID.randomUUID(), ClaimToken.generate()));

        assignment.updatePresence(DeviceStatus.OFFLINE, Instant.parse("2026-05-16T22:30:00Z"));
        assertThat(assignment.getLastSeenAt()).isNull();

        assignment.updatePresence(DeviceStatus.ONLINE, Instant.parse("2026-05-16T22:31:00Z"));
        var saved = repository.save(assignment);
        assertThat(saved.getLastSeenAt()).isEqualTo(Instant.parse("2026-05-16T22:31:00Z"));
        assertThat(saved.getPresenceAt()).isEqualTo(Instant.parse("2026-05-16T22:31:00Z"));
    }

    @Test
    void unlinkingRemovesOnlyTheAssignmentRow() {
        UUID deviceId = UUID.randomUUID();
        var assignment = repository.save(new DeviceAssignment(deviceId, ClaimToken.generate()));

        repository.deleteById(assignment.getId());

        assertThat(repository.findByDeviceId(deviceId)).isEmpty();
    }
}
