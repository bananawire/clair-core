package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.domain.model.valueobjects.DeviceMetricThresholdConfiguration;
import com.claircore.device.domain.model.valueobjects.MetricThreshold;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.domain.model.commands.RemoveDeviceThresholdCommand;
import com.claircore.device.domain.model.commands.WriteDeviceThresholdCommand;
import com.claircore.device.domain.model.queries.GetDeviceThresholdsByDeviceQuery;
import com.claircore.device.application.commandservices.DeviceThresholdCommandService;
import com.claircore.device.application.queryservices.DeviceThresholdQueryService;
import com.claircore.device.interfaces.rest.resources.UpdateDeviceThresholdRequest;
import com.claircore.iam.application.queryservices.TokenQueryService;
import com.claircore.shared.interfaces.rest.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeviceThresholdController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DeviceThresholdControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DeviceThresholdCommandService deviceThresholdCommandService;

    @MockitoBean
    private DeviceThresholdQueryService deviceThresholdQueryService;

    @MockitoBean
    private TokenQueryService tokenQueryService;

    @BeforeEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnThresholdsWhenDeviceHasConfigurations() throws Exception {
        authenticate("550e8400-e29b-41d4-a716-446655444000");
        UUID deviceId = UUID.fromString("550e8400-e29b-41d4-a716-446655444001");
        DeviceMetricThresholdConfiguration configuration = new DeviceMetricThresholdConfiguration(MetricThreshold.PM25, new BigDecimal("35.5"), true);
        when(deviceThresholdQueryService.handle(org.mockito.ArgumentMatchers.any(GetDeviceThresholdsByDeviceQuery.class))).thenReturn(List.of(configuration));

        mockMvc.perform(get("/api/v1/devices/{deviceId}/thresholds", deviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].metric").value("PM25"));
    }

    @Test
    void shouldCreateThresholdWhenRequestIsValid() throws Exception {
        authenticate("550e8400-e29b-41d4-a716-446655444000");
        UUID deviceId = UUID.fromString("550e8400-e29b-41d4-a716-446655444002");
        DeviceMetricThresholdConfiguration configuration = new DeviceMetricThresholdConfiguration(MetricThreshold.CO2, new BigDecimal("800"), true);
        when(deviceThresholdCommandService.handle(org.mockito.ArgumentMatchers.any(WriteDeviceThresholdCommand.class))).thenReturn(configuration);

        mockMvc.perform(post("/api/v1/devices/{deviceId}/thresholds", deviceId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new UpdateDeviceThresholdRequest(MetricThreshold.CO2, new BigDecimal("800"), true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.metric").value("CO2"));

        verify(deviceThresholdCommandService).handle(org.mockito.ArgumentMatchers.any(WriteDeviceThresholdCommand.class));
    }

    @Test
    void shouldReturnBadRequestWhenThresholdRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/devices/{deviceId}/thresholds", UUID.randomUUID())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new UpdateDeviceThresholdRequest(null, null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnForbiddenWhenRemovingThresholdWithoutAuthentication() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(delete("/api/v1/devices/{deviceId}/thresholds/{metric}", UUID.randomUUID(), MetricThreshold.PM25))
                .andExpect(status().isForbidden());
    }

    private void authenticate(String userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new User(userId, "N/A", Collections.emptyList()), null, Collections.emptyList())
        );
    }
}
