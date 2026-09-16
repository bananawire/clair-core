package com.claircore.analytics.interfaces.rest.controllers;

import com.claircore.analytics.application.internal.outboundservices.acl.ExternalBillingService;
import com.claircore.analytics.application.internal.outboundservices.acl.ExternalDeviceService;
import com.claircore.analytics.domain.model.aggregates.DeviceDailySummary;
import com.claircore.analytics.domain.model.aggregates.DeviceMonthlySummary;
import com.claircore.analytics.domain.model.queries.GetDailyReportQuery;
import com.claircore.analytics.domain.model.queries.GetMonthlyReportQuery;
import com.claircore.analytics.domain.model.valueobjects.AqiCategory;
import com.claircore.analytics.domain.model.valueobjects.AqiCategoryBreakdown;
import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.domain.model.valueobjects.MetricStats;
import com.claircore.analytics.application.queryservices.DailyReportQueryService;
import com.claircore.analytics.application.queryservices.MonthlyReportQueryService;
import com.claircore.shared.interfaces.rest.security.CurrentUserIdArgumentResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ReportController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class}
)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DailyReportQueryService dailyReportQueryService;

    @MockitoBean
    private MonthlyReportQueryService monthlyReportQueryService;

    @MockitoBean
    private ExternalDeviceService externalDeviceService;

    @MockitoBean
    private ExternalBillingService externalBillingService;

    @MockitoBean
    private com.claircore.iam.application.queryservices.TokenQueryService tokenQueryService;

    @MockitoBean
    private com.claircore.iam.infrastructure.tokens.jwt.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private com.claircore.iam.infrastructure.config.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @org.junit.jupiter.api.BeforeEach
    void setUpSecurity() throws Exception {
        org.mockito.Mockito.doAnswer(invocation -> {
            jakarta.servlet.ServletRequest request = invocation.getArgument(0);
            jakarta.servlet.ServletResponse response = invocation.getArgument(1);
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    void shouldReturnDailyReportWhenOwner() throws Exception {
        var userId = UUID.randomUUID();
        var deviceId = UUID.randomUUID();
        var summaryDate = LocalDate.now();

        when(externalDeviceService.isDeviceOwnedByUser(deviceId, userId)).thenReturn(true);

        var co2 = MetricStats.of(450.0, 300.0, 600.0);
        var pm25 = MetricStats.of(12.0, 5.0, 20.0);
        var temp = MetricStats.of(22.0, 18.0, 25.0);
        var hum = MetricStats.of(50.0, 40.0, 60.0);

        var dailySummary = new DeviceDailySummary(
                new DeviceId(deviceId),
                summaryDate,
                co2, pm25, temp, hum,
                20.0, Instant.now(),
                55, AqiCategory.MODERATE,
                new AqiCategoryBreakdown(100, 50, 0, 0, 0, 0),
                150, 2.5
        );

        when(dailyReportQueryService.handle(any(GetDailyReportQuery.class)))
                .thenReturn(Optional.of(dailySummary));

        mockMvc.perform(get("/api/v1/analytics/devices/{deviceId}/reports/daily", deviceId)
                        .requestAttr(CurrentUserIdArgumentResolver.USER_ID_ATTRIBUTE, userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageAqi").value(55));
    }

    @Test
    void shouldReturn403WhenDeviceNotOwned() throws Exception {
        var userId = UUID.randomUUID();
        var deviceId = UUID.randomUUID();

        when(externalDeviceService.isDeviceOwnedByUser(deviceId, userId)).thenReturn(false);

        mockMvc.perform(get("/api/v1/analytics/devices/{deviceId}/reports/daily", deviceId)
                        .requestAttr(CurrentUserIdArgumentResolver.USER_ID_ATTRIBUTE, userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnMonthlyReportWhenPremiumSubscriber() throws Exception {
        var userId = UUID.randomUUID();
        var deviceId = UUID.randomUUID();
        var summaryMonth = LocalDate.now().withDayOfMonth(1);

        when(externalDeviceService.isDeviceOwnedByUser(deviceId, userId)).thenReturn(true);
        when(externalBillingService.canAccessMonthlyReports(userId)).thenReturn(true);

        var co2 = MetricStats.of(450.0, 300.0, 600.0);
        var pm25 = MetricStats.of(12.0, 5.0, 20.0);
        var temp = MetricStats.of(22.0, 18.0, 25.0);
        var hum = MetricStats.of(50.0, 40.0, 60.0);

        var monthlySummary = new DeviceMonthlySummary(
                new DeviceId(deviceId),
                summaryMonth,
                co2, pm25, temp, hum,
                20.0, Instant.now(),
                55, AqiCategory.MODERATE,
                new AqiCategoryBreakdown(100, 50, 0, 0, 0, 0),
                150, 30, 2.5
        );

        when(monthlyReportQueryService.handle(any(GetMonthlyReportQuery.class)))
                .thenReturn(Optional.of(monthlySummary));

        mockMvc.perform(get("/api/v1/analytics/devices/{deviceId}/reports/monthly", deviceId)
                        .requestAttr(CurrentUserIdArgumentResolver.USER_ID_ATTRIBUTE, userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageAqi").value(55));
    }

    @Test
    void shouldReturn403WhenMonthlyReportWithoutPremium() throws Exception {
        var userId = UUID.randomUUID();
        var deviceId = UUID.randomUUID();

        when(externalDeviceService.isDeviceOwnedByUser(deviceId, userId)).thenReturn(true);
        when(externalBillingService.canAccessMonthlyReports(userId)).thenReturn(false);

        mockMvc.perform(get("/api/v1/analytics/devices/{deviceId}/reports/monthly", deviceId)
                        .requestAttr(CurrentUserIdArgumentResolver.USER_ID_ATTRIBUTE, userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
