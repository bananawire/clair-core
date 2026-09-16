package com.claircore.analytics.interfaces.rest.controllers;

import com.claircore.analytics.interfaces.rest.sse.AnalyticsSseService;
import com.claircore.analytics.domain.model.queries.GetDashboardMetricsQuery;
import com.claircore.analytics.domain.model.queries.GetHistoricalTrendQuery;
import com.claircore.analytics.domain.model.valueobjects.AirQualityIndex;
import com.claircore.analytics.domain.model.valueobjects.AqiCategory;
import com.claircore.analytics.domain.model.valueobjects.KpiDashboardMetrics;
import com.claircore.analytics.domain.model.valueobjects.MetricTrend;
import com.claircore.analytics.application.queryservices.KpiDashboardMetricsQueryService;
import com.claircore.analytics.application.queryservices.KpiHistoricalTrendQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AnalyticsController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class}
)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KpiDashboardMetricsQueryService kpiDashboardMetricsQueryService;

    @MockitoBean
    private KpiHistoricalTrendQueryService kpiHistoricalTrendQueryService;

    @MockitoBean
    private AnalyticsSseService analyticsSseService;

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
    void shouldReturnLiveMetricsWhenDeviceHasData() throws Exception {
        var deviceId = UUID.randomUUID();
        var metrics = new KpiDashboardMetrics(
                new AirQualityIndex(55, AqiCategory.MODERATE),
                450.0,
                12.5,
                23.0,
                48.0,
                new MetricTrend(0.0, 0.0, 0.0),
                new MetricTrend(0.0, 0.0, 0.0),
                new MetricTrend(0.0, 0.0, 0.0),
                new MetricTrend(0.0, 0.0, 0.0),
                Instant.now()
        );

        when(kpiDashboardMetricsQueryService.handle(any(GetDashboardMetricsQuery.class)))
                .thenReturn(Optional.of(metrics));

        mockMvc.perform(get("/api/v1/analytics/devices/{deviceId}/live", deviceId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aqiValue").value(55))
                .andExpect(jsonPath("$.averageCo2").value(450.0))
                .andExpect(jsonPath("$.averagePm2_5").value(12.5));
    }

    @Test
    void shouldReturn404WhenLiveMetricsUnavailable() throws Exception {
        var deviceId = UUID.randomUUID();
        when(kpiDashboardMetricsQueryService.handle(any(GetDashboardMetricsQuery.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/analytics/devices/{deviceId}/live", deviceId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnHistoricalMetricsWhenValidRequest() throws Exception {
        var deviceId = UUID.randomUUID();
        var metrics = new KpiDashboardMetrics(
                new AirQualityIndex(55, AqiCategory.MODERATE),
                450.0,
                12.5,
                23.0,
                48.0,
                new MetricTrend(0.0, 0.0, 0.0),
                new MetricTrend(0.0, 0.0, 0.0),
                new MetricTrend(0.0, 0.0, 0.0),
                new MetricTrend(0.0, 0.0, 0.0),
                Instant.now()
        );

        when(kpiDashboardMetricsQueryService.handle(any(GetDashboardMetricsQuery.class)))
                .thenReturn(Optional.of(metrics));

        mockMvc.perform(get("/api/v1/analytics/devices/{deviceId}/historical", deviceId)
                        .param("period", "DAY")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aqiValue").value(55));
    }

    @Test
    void shouldReturn400WhenPeriodIsNotARecognisedWindow() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/devices/{deviceId}/historical", UUID.randomUUID())
                        .param("period", "fortnight")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAcceptALowercasePeriodAsItAlwaysHas() throws Exception {
        when(kpiHistoricalTrendQueryService.handle(any(GetHistoricalTrendQuery.class)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/analytics/devices/{deviceId}/trends", UUID.randomUUID())
                        .param("period", "week")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn400WhenHistoricalStartDateAfterEndDate() throws Exception {
        var deviceId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/analytics/devices/{deviceId}/historical", deviceId)
                        .param("startDate", "2026-06-05T22:00:00Z")
                        .param("endDate", "2026-06-05T21:00:00Z")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnTrendsWhenValidRequest() throws Exception {
        var deviceId = UUID.randomUUID();
        when(kpiHistoricalTrendQueryService.handle(any(GetHistoricalTrendQuery.class)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/analytics/devices/{deviceId}/trends", deviceId)
                        .param("period", "DAY")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataPoints").isArray());
    }

    @Test
    void shouldReturn400WhenTrendsStartDateAfterEndDate() throws Exception {
        var deviceId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/analytics/devices/{deviceId}/trends", deviceId)
                        .param("startDate", "2026-06-05T22:00:00Z")
                        .param("endDate", "2026-06-05T21:00:00Z")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
