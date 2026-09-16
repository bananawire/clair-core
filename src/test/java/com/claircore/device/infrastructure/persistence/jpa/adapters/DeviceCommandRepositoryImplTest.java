package com.claircore.device.infrastructure.persistence.jpa.adapters;

import com.claircore.device.domain.model.aggregates.Device;
import com.claircore.device.domain.model.aggregates.DeviceCommand;
import com.claircore.device.domain.model.valueobjects.ApiKey;
import com.claircore.device.domain.model.valueobjects.DeviceCommandStatus;
import com.claircore.device.domain.model.valueobjects.DeviceCommandType;
import com.claircore.device.domain.model.valueobjects.DeviceType;
import com.claircore.device.domain.model.valueobjects.HardwareId;
import com.claircore.device.domain.repositories.DeviceCommandRepository;
import com.claircore.device.domain.repositories.DeviceRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceCommandPersistenceRepository;
import com.claircore.shared.infrastructure.config.JpaAuditingConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The dialect override matters: the app pins PostgreSQLDialect, whose pessimistic lock renders as
 * {@code FOR NO KEY UPDATE}, which H2 cannot parse. Running these against H2's own dialect exercises
 * the ordering and filtering; that the lock is actually requested is asserted separately below,
 * since no in-memory database can prove it.
 */
@DataJpaTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
@Import({JpaAuditingConfiguration.class, DeviceCommandRepositoryImpl.class, DeviceRepositoryImpl.class, DeviceAssignmentRepositoryImpl.class})
class DeviceCommandRepositoryImplTest {

    @Autowired
    private DeviceCommandRepository commands;

    @Autowired
    private DeviceRepository devices;
    @Autowired
    private com.claircore.device.domain.repositories.DeviceAssignmentRepository assignments;

    @Test
    void bothPendingQueriesTakeAPessimisticWriteLock() throws NoSuchMethodException {
        for (String name : new String[]{"findPendingForEdge", "findPendingForEdgeByDevice"}) {
            var method = java.util.Arrays.stream(DeviceCommandPersistenceRepository.class.getMethods())
                    .filter(m -> m.getName().equals(name)).findFirst().orElseThrow();
            assertThat(method.getAnnotation(org.springframework.data.jpa.repository.Lock.class))
                    .isNotNull()
                    .extracting(org.springframework.data.jpa.repository.Lock::value)
                    .isEqualTo(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        }
    }

    @Test
    void roundTripsEveryFieldThroughStorage() {
        var device = saveDevice("SN-1", "HW-0001");
        var command = commands.save(new DeviceCommand(device.getId(), DeviceCommandType.WAKE, "{\"a\":1}"));

        var found = commands.findById(command.getId()).orElseThrow();

        assertThat(found.getDeviceId()).isEqualTo(device.getId());
        assertThat(found.getType()).isEqualTo(DeviceCommandType.WAKE);
        assertThat(found.getStatus()).isEqualTo(DeviceCommandStatus.PENDING);
        assertThat(found.getPayload()).isEqualTo("{\"a\":1}");
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    void anExpiredSentCommandIsRedeliveredAheadOfANewerPendingOne() {
        var device = saveDevice("SN-2", "HW-0002");

        var expired = new DeviceCommand(device.getId(), DeviceCommandType.WAKE, "{}");
        expired.markSent();
        ReflectionTestUtils.setField(expired, "sentAt", Instant.parse("2024-01-01T00:00:00Z"));
        commands.save(expired);

        var pending = commands.save(new DeviceCommand(device.getId(), DeviceCommandType.WAKE, "{}"));

        var result = commands.findPendingForEdge(null, Instant.parse("2024-06-01T00:00:00Z"), 10);

        // Ordered by the oldest delivery or creation time, so an in-flight command cannot starve.
        assertThat(result).extracting(DeviceCommand::getId)
                .containsExactly(expired.getId(), pending.getId());
    }

    @Test
    void aSentCommandWithinItsLeaseIsNotOfferedAgain() {
        var device = saveDevice("SN-3", "HW-0003");
        var command = new DeviceCommand(device.getId(), DeviceCommandType.WAKE, "{}");
        command.markSent();
        commands.save(command);

        var result = commands.findPendingForEdge(null, Instant.now().minusSeconds(300), 10);

        assertThat(result).isEmpty();
    }

    @Test
    void theHardwareFilterIsAppliedBeforeTheLimit() {
        var other = saveDevice("SN-4", "HW-0004");
        var wanted = saveDevice("SN-5", "HW-0005");
        commands.save(new DeviceCommand(other.getId(), DeviceCommandType.WAKE, "{}"));
        commands.save(new DeviceCommand(wanted.getId(), DeviceCommandType.WAKE, "{}"));

        var result = commands.findPendingForEdgeByHardware(
                "HW-0005", null, Instant.now().plusSeconds(1), 1);

        assertThat(result).singleElement()
                .extracting(DeviceCommand::getDeviceId).isEqualTo(wanted.getId());
    }

    @Test
    void anUnknownHardwareIdHasNoCommandsRatherThanEveryones() {
        var device = saveDevice("SN-6", "HW-0006");
        commands.save(new DeviceCommand(device.getId(), DeviceCommandType.WAKE, "{}"));

        assertThat(commands.findPendingForEdgeByHardware("HW-9999", null, Instant.now(), 10)).isEmpty();
    }

    @Test
    void onlyOneCallerCanClaimTheSameCommand() {
        var device = saveDevice("SN-7", "HW-0007");
        var command = commands.save(new DeviceCommand(device.getId(), DeviceCommandType.WAKE, "{}"));
        Instant now = Instant.now();
        Instant cutoff = now.minusSeconds(300);

        assertThat(commands.claimForEdge(command.getId(), cutoff, now)).isEqualTo(1);
        assertThat(commands.claimForEdge(command.getId(), cutoff, now)).isZero();
    }

    @Test
    void anExpiredSentCommandCanBeClaimedAgainAfterItsLease() {
        var device = saveDevice("SN-7B", "HW-0007");
        var command = commands.save(new DeviceCommand(device.getId(), DeviceCommandType.WAKE, "{}"));
        Instant firstClaim = Instant.parse("2026-05-16T22:30:00Z");

        assertThat(commands.claimForEdge(command.getId(), firstClaim.minusSeconds(1), firstClaim)).isEqualTo(1);
        assertThat(commands.claimForEdge(
                command.getId(), firstClaim.plusSeconds(60), firstClaim.plusSeconds(61))).isEqualTo(1);
        assertThat(commands.findById(command.getId())).get()
                .extracting(DeviceCommand::getStatus).isEqualTo(DeviceCommandStatus.SENT);
    }

    @Test
    void latestByDeviceIdIsTheMostRecentlyCreatedCommand() {
        var device = saveDevice("SN-8", "HW-0008");
        commands.save(new DeviceCommand(device.getId(), DeviceCommandType.WAKE, "{}"));
        var newest = commands.save(new DeviceCommand(device.getId(), DeviceCommandType.STANDBY, "{}"));

        assertThat(commands.findLatestByDeviceId(device.getId()))
                .map(DeviceCommand::getId).contains(newest.getId());
    }

    @Test
    void aCommandBoundToAnUnlinkedAssignmentIsNeverOfferedToTheEdge() {
        var device = saveDevice("SN-9", "HW-0009");
        var assignment = assignments.save(new com.claircore.device.domain.model.aggregates.DeviceAssignment(
                device.getId(), com.claircore.device.domain.model.valueobjects.ClaimToken.generate()));
        var bound = commands.save(new DeviceCommand(device.getId(), assignment.getId(), DeviceCommandType.WAKE, "{}"));
        assertThat(commands.findPendingForEdge(null, Instant.now(), 10)).extracting(DeviceCommand::getId)
                .contains(bound.getId());
        assignments.deleteById(assignment.getId());
        assertThat(commands.findPendingForEdge(null, Instant.now(), 10)).extracting(DeviceCommand::getId)
                .doesNotContain(bound.getId());
        assertThat(commands.findPendingForEdgeByHardware("HW-0009", null, Instant.now(), 10)).isEmpty();
    }

    @Test
    void aCommandCannotBeClaimedWhenItsAssignmentBelongsToAnotherDevice() {
        var assignmentDevice = saveDevice("SN-9A", "HW-0009");
        var commandDevice = saveDevice("SN-9B", "HW-0011");
        var assignment = assignments.save(new com.claircore.device.domain.model.aggregates.DeviceAssignment(
                assignmentDevice.getId(), com.claircore.device.domain.model.valueobjects.ClaimToken.generate()));
        var mismatched = commands.save(new DeviceCommand(
                commandDevice.getId(), assignment.getId(), DeviceCommandType.WAKE, "{}"));

        assertThat(commands.findPendingForEdge(null, Instant.now(), 10))
                .extracting(DeviceCommand::getId).doesNotContain(mismatched.getId());
        assertThat(commands.findPendingForEdgeByHardware(
                "HW-0011", null, Instant.now(), 10)).isEmpty();
        assertThat(commands.claimForEdge(mismatched.getId(), Instant.now(), Instant.now())).isZero();
    }

    @Test
    void expiringAnAssignmentVoidsOnlyItsOutstandingCommands() {
        var device = saveDevice("SN-10", "HW-0010");
        var assignment = assignments.save(new com.claircore.device.domain.model.aggregates.DeviceAssignment(
                device.getId(), com.claircore.device.domain.model.valueobjects.ClaimToken.generate()));
        var pending = commands.save(new DeviceCommand(device.getId(), assignment.getId(), DeviceCommandType.WAKE, "{}"));
        var sent = new DeviceCommand(device.getId(), assignment.getId(), DeviceCommandType.STANDBY, "{}");
        sent.markSent();
        sent = commands.save(sent);
        var done = new DeviceCommand(device.getId(), assignment.getId(), DeviceCommandType.RESTART, "{}");
        done.markExecuted();
        done = commands.save(done);
        var other = commands.save(new DeviceCommand(device.getId(), UUID.randomUUID(), DeviceCommandType.WAKE, "{}"));
        assertThat(commands.expireOutstandingByAssignmentId(assignment.getId())).isEqualTo(2);
        assertThat(commands.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(DeviceCommandStatus.EXPIRED);
        assertThat(commands.findById(sent.getId()).orElseThrow().getStatus()).isEqualTo(DeviceCommandStatus.EXPIRED);
        assertThat(commands.findById(done.getId()).orElseThrow().getStatus()).isEqualTo(DeviceCommandStatus.EXECUTED);
        assertThat(commands.findById(other.getId()).orElseThrow().getStatus()).isEqualTo(DeviceCommandStatus.PENDING);
    }

    private Device saveDevice(String serial, String hardware) {
        return devices.save(new Device(serial, "Sensor", new HardwareId(hardware),
                ApiKey.generate(), new DeviceType("air-quality-v1")));
    }
}
