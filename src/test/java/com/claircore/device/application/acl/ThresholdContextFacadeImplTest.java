package com.claircore.device.application.acl;

import com.claircore.device.application.queryservices.DeviceThresholdQueryService;
import com.claircore.device.domain.model.valueobjects.DeviceMetricThresholdConfiguration;
import com.claircore.device.domain.model.valueobjects.MetricThreshold;
import com.claircore.device.interfaces.acl.ThresholdSummary;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ThresholdContextFacadeImplTest {
    @Test
    void exposesOnlyPublishedSummariesFromTheQueryService() {
        var queries = mock(DeviceThresholdQueryService.class);
        var deviceId = UUID.randomUUID();
        when(queries.findEnabledByDeviceId(deviceId)).thenReturn(List.of(
            new DeviceMetricThresholdConfiguration(MetricThreshold.PM25, new BigDecimal("35.5"), true)));
        assertThat(new ThresholdContextFacadeImpl(queries).findEnabledThresholdsByDeviceId(deviceId))
            .containsExactly(new ThresholdSummary("PM25", new BigDecimal("35.5"), true));
        verify(queries).findEnabledByDeviceId(deviceId);
    }
}
