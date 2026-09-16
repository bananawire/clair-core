package com.claircore.alerting.application.internal.queryservices;

import com.claircore.alerting.application.internal.outboundservices.acl.ExternalAlertingDeviceService;
import com.claircore.alerting.domain.model.aggregates.Alert;
import com.claircore.alerting.domain.model.queries.GetAlertsByOwnerQuery;
import com.claircore.alerting.domain.model.valueobjects.AlertSeverity;
import com.claircore.alerting.domain.model.valueobjects.MetricType;
import com.claircore.alerting.domain.repositories.AlertRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertQueryServiceImplTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private ExternalAlertingDeviceService externalDeviceService;

    @InjectMocks
    private AlertQueryServiceImpl service;

    private static final Instant OCCURRED_AT = Instant.parse("2026-05-16T22:30:00Z");

    @Test
    void returnsAnEmptyPageWithoutQueryingWhenTheUserOwnsNoDevices() {
        var query = new GetAlertsByOwnerQuery(UUID.randomUUID(), 0, 20);

        var page = service.fetchByOwner(query, List.of());

        assertThat(page.items()).isEmpty();
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(20);
        verify(alertRepository, never()).findByDeviceIdIn(anyCollection(), anyInt(), anyInt());
    }

    @Test
    void fallsBackToTheUnfilteredQueryWhenNoStatusesAreGiven() {
        UUID deviceId = UUID.randomUUID();
        var query = new GetAlertsByOwnerQuery(UUID.randomUUID(), 0, 20);
        when(alertRepository.findByDeviceIdIn(List.of(deviceId), 0, 20))
                .thenReturn(new com.claircore.shared.domain.model.PageResult<>(List.of(alert(deviceId)), 0, 20, 1));

        var page = service.fetchByOwnerAndStatus(query, List.of(deviceId), List.of());

        assertThat(page.total()).isEqualTo(1);
        verify(alertRepository, never())
                .findByDeviceIdInAndStatusIn(anyCollection(), anyCollection(), anyInt(), anyInt());
    }

    private static Alert alert(UUID deviceId) {
        return new Alert(deviceId, null, null, null, MetricType.PM25,
                new BigDecimal("50.00"), new BigDecimal("80.00"), "PM2.5 threshold exceeded",
                AlertSeverity.CRITICAL, OCCURRED_AT);
    }
}
