package com.claircore.analytics.infrastructure.persistence.jpa.adapters;

import com.claircore.analytics.domain.model.aggregates.DeviceAnalyticsSnapshot;
import com.claircore.analytics.domain.model.valueobjects.AirQualityIndex;
import com.claircore.analytics.domain.model.valueobjects.AqiCategory;
import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.domain.repositories.DeviceAnalyticsSnapshotRepository;
import com.claircore.shared.infrastructure.config.JpaAuditingConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DataJpaTest
@Import({JpaAuditingConfiguration.class, DeviceAnalyticsSnapshotRepositoryImpl.class})
class DeviceAnalyticsSnapshotRepositoryImplTest {

    private static final Instant HOUR = Instant.parse("2026-05-16T22:00:00Z");

    @Autowired
    private DeviceAnalyticsSnapshotRepository repository;

    @Test
    void savesWithTheIdentityTheAggregateAssignedAndFillsTheAuditTimestamps() {
        var snapshot = snapshot(UUID.randomUUID(), HOUR, 450.0, 12.0, 55);

        var saved = repository.save(snapshot);

        assertThat(saved.getId()).isEqualTo(snapshot.getId());
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void roundTripsEveryFieldThroughStorage() {
        UUID deviceId = UUID.randomUUID();

        repository.save(snapshot(deviceId, HOUR, 450.0, 12.0, 55));
        var found = repository.findLatestByDeviceId(deviceId).orElseThrow();

        assertThat(found.getDeviceId()).isEqualTo(new DeviceId(deviceId));
        assertThat(found.getTimeWindowStart()).isEqualTo(HOUR.minus(1, ChronoUnit.HOURS));
        assertThat(found.getTimeWindowEnd()).isEqualTo(HOUR);
        assertThat(found.getAverageCo2()).isEqualTo(450.0);
        assertThat(found.getAveragePm2_5()).isEqualTo(12.0);
        assertThat(found.getAverageTemperature()).isEqualTo(23.5);
        assertThat(found.getAverageHumidity()).isEqualTo(52.0);
        assertThat(found.getCalculatedAqi()).isEqualTo(new AirQualityIndex(55, AqiCategory.MODERATE));
    }

    @Test
    void latestByDeviceIdIsTheOneWithTheNewestWindowEnd() {
        UUID deviceId = UUID.randomUUID();
        repository.save(snapshot(deviceId, HOUR, 450.0, 12.0, 55));
        repository.save(snapshot(deviceId, HOUR.plus(1, ChronoUnit.HOURS), 900.0, 40.0, 120));

        var latest = repository.findLatestByDeviceId(deviceId).orElseThrow();

        assertThat(latest.getAverageCo2()).isEqualTo(900.0);
    }

    @Test
    void latestByDeviceIdsReturnsOneRowPerDeviceInOneCall() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        repository.save(snapshot(first, HOUR, 450.0, 12.0, 55));
        repository.save(snapshot(first, HOUR.plus(1, ChronoUnit.HOURS), 900.0, 40.0, 120));
        repository.save(snapshot(second, HOUR, 400.0, 8.0, 30));

        var latest = repository.findLatestByDeviceIds(List.of(first, second));

        assertThat(latest).hasSize(2);
        assertThat(latest).extracting(DeviceAnalyticsSnapshot::getAverageCo2)
                .containsExactlyInAnyOrder(900.0, 400.0);
    }

    @Test
    void latestByDeviceIdsIsEmptyForAnEmptyRequestWithoutTouchingStorage() {
        assertThat(repository.findLatestByDeviceIds(List.of())).isEmpty();
    }

    @Test
    void windowIsHalfOpenAndOrderedOldestFirst() {
        UUID deviceId = UUID.randomUUID();
        repository.save(snapshot(deviceId, HOUR, 450.0, 12.0, 55));
        repository.save(snapshot(deviceId, HOUR.plus(1, ChronoUnit.HOURS), 900.0, 40.0, 120));
        repository.save(snapshot(deviceId, HOUR.plus(2, ChronoUnit.HOURS), 500.0, 15.0, 60));

        // Windows open at 21:00, 22:00 and 23:00; [21:00, 23:00) takes the first two.
        var found = repository.findByDeviceIdAndWindowStartBetween(
                deviceId, HOUR.minus(1, ChronoUnit.HOURS), HOUR.plus(1, ChronoUnit.HOURS), null);

        assertThat(found).extracting(DeviceAnalyticsSnapshot::getAverageCo2).containsExactly(450.0, 900.0);
    }

    @Test
    void aLimitTakesTheOldestSnapshotsInTheWindow() {
        UUID deviceId = UUID.randomUUID();
        repository.save(snapshot(deviceId, HOUR, 450.0, 12.0, 55));
        repository.save(snapshot(deviceId, HOUR.plus(1, ChronoUnit.HOURS), 900.0, 40.0, 120));

        var found = repository.findByDeviceIdAndWindowStartBetween(
                deviceId, HOUR.minus(5, ChronoUnit.HOURS), HOUR.plus(5, ChronoUnit.HOURS), 1);

        assertThat(found).extracting(DeviceAnalyticsSnapshot::getAverageCo2).containsExactly(450.0);
    }

    @Test
    void averagesAcrossTheWindowAreTheMeanOfEachMetric() {
        UUID deviceId = UUID.randomUUID();
        repository.save(snapshot(deviceId, HOUR, 400.0, 10.0, 50));
        repository.save(snapshot(deviceId, HOUR.plus(1, ChronoUnit.HOURS), 500.0, 20.0, 60));

        var averages = repository.findAveragesByDeviceIdAndWindow(
                deviceId, HOUR.minus(5, ChronoUnit.HOURS), HOUR.plus(5, ChronoUnit.HOURS)).orElseThrow();

        assertThat(averages.co2()).isCloseTo(450.0, within(0.001));
        assertThat(averages.pm2_5()).isCloseTo(15.0, within(0.001));
        assertThat(averages.temperature()).isCloseTo(23.5, within(0.001));
        assertThat(averages.humidity()).isCloseTo(52.0, within(0.001));
    }

    @Test
    void averagesAreEmptyRatherThanZeroWhenTheWindowHoldsNoSnapshot() {
        var averages = repository.findAveragesByDeviceIdAndWindow(
                UUID.randomUUID(), HOUR, HOUR.plus(1, ChronoUnit.HOURS));

        assertThat(averages).isEmpty();
    }

    private static DeviceAnalyticsSnapshot snapshot(
            UUID deviceId, Instant windowEnd, double co2, double pm2_5, int aqi) {
        return new DeviceAnalyticsSnapshot(
                new DeviceId(deviceId),
                windowEnd.minus(1, ChronoUnit.HOURS),
                windowEnd,
                co2,
                pm2_5,
                23.5,
                52.0,
                new AirQualityIndex(aqi, aqi <= 50 ? AqiCategory.GOOD
                        : aqi <= 100 ? AqiCategory.MODERATE : AqiCategory.UNHEALTHY_FOR_SENSITIVE));
    }
}
