package com.claircore.analytics.interfaces.rest.controllers;

import com.claircore.analytics.domain.model.queries.GetOverviewDashboardQuery;
import com.claircore.analytics.domain.model.valueobjects.OverviewDashboardSnapshot;
import com.claircore.analytics.domain.model.valueobjects.OverviewDashboardSnapshot.OverviewCoreMetrics;
import com.claircore.analytics.application.queryservices.OverviewDashboardQueryService;
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
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AnalyticsOverviewController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class}
)
class AnalyticsOverviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OverviewDashboardQueryService overviewDashboardQueryService;

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
    void shouldReturnOverviewWhenAuthorized() throws Exception {
        var userId = UUID.randomUUID();
        var coreMetrics = new OverviewCoreMetrics(
                55, "MODERATE", 450.0, 12.5, 23.0, 48.0,
                1.5, 2.5, 0.5, -1.0, Instant.now(),
                2, 4, 10, "LIVE"
        );
        var snapshot = new OverviewDashboardSnapshot(
                coreMetrics,
                Collections.emptyList(),
                Collections.emptyList(),
                Instant.now()
        );

        when(overviewDashboardQueryService.handle(any(GetOverviewDashboardQuery.class)))
                .thenReturn(snapshot);

        mockMvc.perform(get("/api/v1/analytics/overview")
                        .requestAttr(CurrentUserIdArgumentResolver.USER_ID_ATTRIBUTE, userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.core.aqiValue").value(55))
                .andExpect(jsonPath("$.core.deviceCount").value(10));
    }
}
