package com.claircore.alerting.infrastructure.persistence.jpa.adapters;

import com.claircore.alerting.domain.model.aggregates.Alert;
import com.claircore.alerting.domain.model.valueobjects.AlertSeverity;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.domain.model.valueobjects.MetricType;
import com.claircore.alerting.domain.repositories.AlertRepository;
import com.claircore.shared.infrastructure.config.JpaAuditingConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the port through its adapter. It also pins what the split removed: the pending-for-edge
 * query no longer joins {@code devices}, so it returns alerts alone and the hardware id is resolved
 * by the query service through the device facade.
 */
@DataJpaTest
@Import({JpaAuditingConfiguration.class, AlertRepositoryImpl.class})
class AlertRepositoryImplTest {

    @Autowired
    private AlertRepository repository;

    private static final Instant OCCURRED_AT = Instant.parse("2026-05-16T22:30:00Z");

    @Test
    void savesWithTheIdentityTheAggregateAssignedAndFillsTheAuditTimestamps() {
        var alert = alert(UUID.randomUUID(), null, OCCURRED_AT);

        var saved = repository.save(alert);

        assertThat(saved.getId()).isEqualTo(alert.getId());
        assertThat(saved.getStatus()).isEqualTo(AlertStatus.ACTIVE);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void roundTripsEveryFieldThroughStorage() {
        UUID deviceId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        var saved = repository.save(alert(deviceId, spaceId, OCCURRED_AT));

        var found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getDeviceId()).isEqualTo(deviceId);
        assertThat(found.getSpaceId()).isEqualTo(spaceId);
        assertThat(found.getMetric()).isEqualTo(MetricType.PM25);
        assertThat(found.getThresholdValue()).isEqualByComparingTo("50.00");
        assertThat(found.getActualValue()).isEqualByComparingTo("80.00");
        assertThat(found.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
        assertThat(found.getSpaceName()).isEqualTo("Floor 2");
        assertThat(found.getDeviceName()).isEqualTo("Living Room");
        assertThat(found.getOccurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(found.getResolvedAt()).isNull();
    }

    @Test
    void pagesMostRecentlyOccurredFirst() {
        UUID deviceId = UUID.randomUUID();
        repository.save(alert(deviceId, null, OCCURRED_AT));
        repository.save(alert(deviceId, null, OCCURRED_AT.plus(1, ChronoUnit.HOURS)));
        repository.save(alert(UUID.randomUUID(), null, OCCURRED_AT));

        var page = repository.findByDeviceId(deviceId, 0, 10);

        assertThat(page.total()).isEqualTo(2);
        assertThat(page.items()).extracting(Alert::getOccurredAt)
                .containsExactly(OCCURRED_AT.plus(1, ChronoUnit.HOURS), OCCURRED_AT);
    }

    @Test
    void filtersByOwnedDevicesAndStatus() {
        UUID owned = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        var resolved = repository.save(alert(owned, null, OCCURRED_AT));
        resolved.resolve(OCCURRED_AT.plusSeconds(60));
        repository.save(resolved);
        repository.save(alert(owned, null, OCCURRED_AT.plus(2, ChronoUnit.HOURS)));
        repository.save(alert(other, null, OCCURRED_AT));

        var active = repository.findByDeviceIdInAndStatusIn(List.of(owned), List.of(AlertStatus.ACTIVE), 0, 10);

        assertThat(active.total()).isEqualTo(1);
        assertThat(active.items().getFirst().getDeviceId()).isEqualTo(owned);
        assertThat(repository.findByDeviceIdAndStatus(owned, AlertStatus.RESOLVED)).hasSize(1);
    }

    @Test
    void findsTheOpenAlertForAMetricSoDuplicatesAreNotCreated() {
        UUID deviceId = UUID.randomUUID();
        repository.save(alert(deviceId, null, OCCURRED_AT));

        assertThat(repository.findFirstByDeviceIdAndMetricAndStatusIn(
                deviceId, MetricType.PM25, List.of(AlertStatus.ACTIVE, AlertStatus.ACKNOWLEDGED))).isPresent();
        assertThat(repository.findFirstByDeviceIdAndMetricAndStatusIn(
                deviceId, MetricType.CO2, List.of(AlertStatus.ACTIVE, AlertStatus.ACKNOWLEDGED))).isEmpty();
    }

    /** Sequence order, bounded, cursor optional; receipts hide what the edge already stored. */
    @Test
    void pagesPendingEdgeAlertsByTransitionSequenceAndHidesReceivedOnes() {
        UUID deviceId = UUID.randomUUID();
        var later = alert(deviceId, null, OCCURRED_AT.plus(1, ChronoUnit.HOURS));
        later.markTransition(repository.nextTransitionSequence());
        later = repository.save(later);
        var earlier = alert(deviceId, null, OCCURRED_AT);
        earlier.markTransition(repository.nextTransitionSequence());
        earlier = repository.save(earlier);
        var statuses = List.of(AlertStatus.ACTIVE, AlertStatus.ACKNOWLEDGED, AlertStatus.RESOLVED);

        var all = repository.findPendingForEdge(statuses, null, 200);
        assertThat(all).extracting(Alert::getId).containsExactly(later.getId(), earlier.getId());
        assertThat(repository.findPendingForEdge(statuses, later.getTransitionSequence(), 200))
                .extracting(Alert::getId).containsExactly(earlier.getId());

        // A receipt for the current transition removes the alert from the unfiltered page...
        earlier.recordEdgeReceipt(earlier.getTransitionSequence());
        earlier = repository.save(earlier);
        assertThat(repository.findPendingForEdge(statuses, null, 200)).extracting(Alert::getId)
                .containsExactly(later.getId());
        // ...until it transitions again, when it reappears with a higher sequence.
        earlier.resolve(OCCURRED_AT.plusSeconds(30));
        earlier.markTransition(repository.nextTransitionSequence());
        repository.save(earlier);
        assertThat(repository.findPendingForEdge(statuses, null, 200)).extracting(Alert::getId)
                .containsExactly(later.getId(), earlier.getId());
    }

    @Test
    void transitionSequencesAreHandedOutStrictlyIncreasing() {
        long first = repository.nextTransitionSequence();
        long second = repository.nextTransitionSequence();
        assertThat(second).isGreaterThan(first);
    }

    @Test
    void countsAlertsPerDay() {
        UUID deviceId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        repository.save(alert(deviceId, spaceId, OCCURRED_AT));
        repository.save(alert(deviceId, spaceId, OCCURRED_AT.plusSeconds(60)));
        repository.save(alert(deviceId, spaceId, OCCURRED_AT.plus(2, ChronoUnit.DAYS)));

        var bySpace = repository.countAlertsPerDayBySpaceId(spaceId, OCCURRED_AT.minus(1, ChronoUnit.DAYS));

        assertThat(bySpace).hasSize(2);
        assertThat(bySpace.getFirst().count()).isEqualTo(2);
        assertThat(repository.countAlertsPerDayByDeviceIds(List.of(deviceId), OCCURRED_AT.minus(1, ChronoUnit.DAYS)))
                .hasSize(2);
    }

    private static Alert alert(UUID deviceId, UUID spaceId, Instant occurredAt) {
        return new Alert(deviceId, spaceId, "Floor 2", "Living Room", MetricType.PM25,
                new BigDecimal("50.00"), new BigDecimal("80.00"), "PM2.5 threshold exceeded",
                AlertSeverity.CRITICAL, occurredAt);
    }
}
