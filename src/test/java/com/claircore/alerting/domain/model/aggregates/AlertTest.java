package com.claircore.alerting.domain.model.aggregates;

import com.claircore.alerting.domain.model.valueobjects.AlertSeverity;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.domain.model.valueobjects.MetricType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AlertTest {

    @Test
    void createsAlertWithActiveStatus() {
        var alert = newAlert();
        assertEquals(AlertStatus.ACTIVE, alert.getStatus());
    }

    @Test
    void createsAlertWithProvidedSeverity() {
        var alert = newAlert();
        assertEquals(AlertSeverity.CRITICAL, alert.getSeverity());
    }

    @Test
    void resolvesAlertAndSetsResolvedAt() {
        var alert = newAlert();
        var now = Instant.now();
        alert.resolve(now);
        assertEquals(AlertStatus.RESOLVED, alert.getStatus());
        assertEquals(now, alert.getResolvedAt());
    }

    @Test
    void resolvesAlertThrowsWhenNullInstant() {
        var alert = newAlert();
        assertThrows(IllegalArgumentException.class, () -> alert.resolve(null));
    }

    @Test
    void createsAlertWithSpaceAndDeviceNames() {
        var alert = newAlert();
        assertEquals("Test Space", alert.getSpaceName());
        assertEquals("Test Device", alert.getDeviceName());
    }

    @Test
    void transitionSequenceOnlyEverIncreases() {
        var alert = newAlert();
        assertEquals(0L, alert.getTransitionSequence());
        alert.markTransition(7);
        assertEquals(7L, alert.getTransitionSequence());
        assertThrows(IllegalArgumentException.class, () -> alert.markTransition(7));
        assertThrows(IllegalArgumentException.class, () -> alert.markTransition(3));
    }

    @Test
    void edgeReceiptIsMonotonicAndCappedAtTheCurrentTransition() {
        var alert = newAlert();
        alert.markTransition(5);
        assertEquals(false, alert.recordEdgeReceipt(3));
        assertEquals(3L, alert.getEdgeReceiptSequence());
        assertEquals(true, alert.recordEdgeReceipt(9));   // ahead of what exists: clamped
        assertEquals(5L, alert.getEdgeReceiptSequence());
        assertEquals(true, alert.recordEdgeReceipt(2));   // older receipt never lowers the watermark
        assertEquals(5L, alert.getEdgeReceiptSequence());
        alert.markTransition(6);
        assertEquals(false, alert.recordEdgeReceipt(5));  // a new transition is pending again
    }

    private static Alert newAlert() {
        return new Alert(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Test Space",
                "Test Device",
                MetricType.PM25,
                new BigDecimal("50.00"),
                new BigDecimal("80.00"),
                "PM2.5 threshold exceeded",
                AlertSeverity.CRITICAL,
                Instant.now()
        );
    }
}
