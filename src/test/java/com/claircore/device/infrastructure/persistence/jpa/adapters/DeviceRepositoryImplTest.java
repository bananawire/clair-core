package com.claircore.device.infrastructure.persistence.jpa.adapters;

import com.claircore.device.domain.model.aggregates.Device;
import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.valueobjects.ApiKey;
import com.claircore.device.domain.model.valueobjects.ClaimToken;
import com.claircore.device.domain.model.valueobjects.DeviceStatus;
import com.claircore.device.domain.model.valueobjects.DeviceType;
import com.claircore.device.domain.model.valueobjects.HardwareId;
import com.claircore.device.domain.model.valueobjects.ProvisionedDevice;
import com.claircore.device.domain.repositories.DeviceAssignmentRepository;
import com.claircore.device.domain.repositories.DeviceRepository;
import com.claircore.shared.infrastructure.config.JpaAuditingConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the edge roster, which is the one query in this context whose behaviour the DDL gate
 * cannot check. The cursor is {@code updatedAt}, and the aggregate no longer forces that column
 * forward itself — auditing does, on the update the mutation causes. These tests pin that the
 * result is the same, because a cursor that stops advancing strands every device behind it.
 */
@DataJpaTest
@Import({JpaAuditingConfiguration.class, DeviceRepositoryImpl.class, DeviceAssignmentRepositoryImpl.class})
class DeviceRepositoryImplTest {

    @Autowired
    private DeviceRepository repository;

    @Autowired
    private DeviceAssignmentRepository assignmentRepository;

    @Test
    void savesWithTheIdentityTheAggregateAssignedAndFillsTheAuditTimestamps() {
        var device = device("SN-1", "HW-0001");

        var saved = repository.save(device);

        assertThat(saved.getId()).isEqualTo(device.getId());
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void roundTripsEveryFieldThroughStorage() {
        var device = repository.save(device("SN-2", "HW-0002"));

        var found = repository.findById(device.getId()).orElseThrow();

        assertThat(found.getSerialNumber()).isEqualTo("SN-2");
        assertThat(found.getName()).isEqualTo("Sensor");
        assertThat(found.getFactoryName()).isEqualTo("Sensor");
        assertThat(found.getHardwareId()).isEqualTo(new HardwareId("HW-0002"));
        var row = repository.findProvisionedDevices(null, null, 10).items().getFirst();
        assertThat(row.hardwareId()).isEqualTo("HW-0002");
        assertThat(row.apiKey()).isEqualTo(device.getApiKey().value());
        assertThat(row.updatedAt()).isCloseTo(found.getUpdatedAt(), org.assertj.core.api.Assertions.within(1, java.time.temporal.ChronoUnit.MICROS));
        assertThat(found.getApiKey()).isEqualTo(device.getApiKey());
        assertThat(found.getDeviceType()).isEqualTo(new DeviceType("air-quality-v1"));
        assertThat(found.isDeleted()).isFalse();
    }

    @Test
    void findsByHardwareIdApiKeyAndSerialNumber() {
        var device = repository.save(device("SN-3", "HW-0003"));

        assertThat(repository.findByHardwareId("HW-0003")).map(Device::getId).contains(device.getId());
        assertThat(repository.findByApiKey(device.getApiKey().value())).map(Device::getId).contains(device.getId());
        assertThat(repository.findBySerialNumber("SN-3")).map(Device::getId).contains(device.getId());
        assertThat(repository.existsByHardwareId("HW-0003")).isTrue();
        assertThat(repository.existsByHardwareId("HW-9999")).isFalse();
    }

    @Test
    void aDecommissionedDeviceStaysInTheRosterAsATombstone() {
        var device = repository.save(device("SN-4", "HW-0004"));
        device.markDeleted();
        repository.save(device);

        var roster = repository.findProvisionedDevices(null, null, 10);

        assertThat(roster.items())
                .filteredOn(row -> row.deviceId().equals(device.getId()))
                .singleElement()
                .extracting(ProvisionedDevice::deleted).isEqualTo(true);
    }

    @Test
    void aDeviceWithNoAssignmentReportsOffline() {
        var device = repository.save(device("SN-5", "HW-0005"));

        var roster = repository.findProvisionedDevices(null, null, 10);

        assertThat(roster.items())
                .filteredOn(row -> row.deviceId().equals(device.getId()))
                .singleElement()
                .extracting(ProvisionedDevice::status).isEqualTo(DeviceStatus.OFFLINE);
    }

    @Test
    void theCursorExcludesEverythingUpToAndIncludingTheRowItNames() {
        var first = repository.save(device("SN-6", "HW-0006"));
        var second = repository.save(device("SN-7", "HW-0007"));

        var page = repository.findProvisionedDevices(first.getUpdatedAt(), first.getId(), 10);

        assertThat(page.items()).extracting(ProvisionedDevice::deviceId).doesNotContain(first.getId());
        assertThat(page.items()).extracting(ProvisionedDevice::deviceId).contains(second.getId());
    }

    @Test
    void renamingADeviceMovesItPastTheCursorThatHadAlreadySeenIt() {
        var device = repository.save(device("SN-8", "HW-0008"));
        var watermark = repository.findProvisionedDevices(null, null, 10).items().getLast().updatedAt();

        // Nothing is newer than the watermark yet.
        assertThat(repository.findProvisionedDevices(watermark, device.getId(), 10).items()).isEmpty();

        device.updateName("Kitchen sensor");
        repository.save(device);

        // The rename must push updated_at forward, or the edge never learns the new name.
        assertThat(repository.findProvisionedDevices(watermark, device.getId(), 10).items())
                .extracting(ProvisionedDevice::deviceId).contains(device.getId());
    }

    @Test
    void theRosterWatermarkFollowsTheAssignmentWhenItIsTheNewerOfTheTwo() {
        var device = repository.save(device("SN-9", "HW-0009"));
        var deviceOnly = repository.findProvisionedDevices(null, null, 10).items().getLast().updatedAt();

        var assignment = assignmentRepository.save(new DeviceAssignment(device.getId(), ClaimToken.generate()));
        assignment.markOnline();
        assignmentRepository.save(assignment);

        var row = repository.findProvisionedDevices(null, null, 10).items().stream()
                .filter(item -> item.deviceId().equals(device.getId())).findFirst().orElseThrow();

        assertThat(row.status()).isEqualTo(DeviceStatus.ONLINE);
        assertThat(row.updatedAt()).isAfterOrEqualTo(deviceOnly);
    }

    @Test
    void resettingAnUnrenamedDeviceRemainsVisibleAfterAssignmentDeletion() {
        var device = repository.save(device("SN-10", "HW-0010"));
        var assignment = assignmentRepository.save(new DeviceAssignment(device.getId(), ClaimToken.generate()));
        assignment.markOnline();
        assignmentRepository.save(assignment);
        var watermark = repository.findProvisionedDevices(null, null, 10).items().getLast().updatedAt();
        device = repository.findById(device.getId()).orElseThrow();
        device.resetNameToFactoryDefault();
        repository.save(device);
        assignmentRepository.deleteById(assignment.getId());
        repository.advanceRosterWatermark(device.getId(), watermark);
        var rows = repository.findProvisionedDevices(watermark, device.getId(), 10).items();
        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.status()).isEqualTo(DeviceStatus.OFFLINE);
            assertThat(row.updatedAt()).isAfter(watermark);
        });
    }

    private static Device device(String serial, String hardware) {
        return new Device(serial, "Sensor", new HardwareId(hardware), ApiKey.generate(),
                new DeviceType("air-quality-v1"));
    }
}
