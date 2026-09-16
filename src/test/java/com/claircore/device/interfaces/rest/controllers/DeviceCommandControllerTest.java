package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.domain.model.aggregates.Device;
import com.claircore.device.domain.model.aggregates.DeviceCommand;
import com.claircore.device.domain.model.commands.CreateDeviceCommandCommand;
import com.claircore.device.domain.model.queries.GetDeviceCommandByIdForUserQuery;
import com.claircore.device.domain.model.queries.GetLatestDeviceCommandByDeviceForUserQuery;
import com.claircore.device.domain.model.valueobjects.*;
import com.claircore.device.application.queryservices.DeviceCommandQueryService;
import com.claircore.device.application.commandservices.DeviceControlCommandService;
import com.claircore.device.interfaces.rest.resources.CreateDeviceCommandRequest;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeviceCommandController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DeviceCommandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DeviceControlCommandService deviceControlCommandService;

    @MockitoBean
    private DeviceCommandQueryService deviceCommandQueryService;

    @MockitoBean
    private TokenQueryService tokenQueryService;

    @BeforeEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnCreatedWhenCommandIsCreated() throws Exception {
        authenticate("550e8400-e29b-41d4-a716-446655443000");
        DeviceCommand command = command();
        when(deviceControlCommandService.handle(org.mockito.ArgumentMatchers.any(CreateDeviceCommandCommand.class))).thenReturn(command);

        mockMvc.perform(post("/api/v1/devices/{deviceId}/commands", command.getDeviceId())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateDeviceCommandRequest(DeviceCommandType.WAKE, "{}"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("WAKE"));

        verify(deviceControlCommandService).handle(org.mockito.ArgumentMatchers.any(CreateDeviceCommandCommand.class));
    }

    @Test
    void shouldReturnForbiddenWhenCreatingCommandWithoutAuthentication() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(post("/api/v1/devices/{deviceId}/commands", UUID.randomUUID())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateDeviceCommandRequest(DeviceCommandType.WAKE, "{}"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnLatestCommandWhenDeviceHasCommands() throws Exception {
        authenticate("550e8400-e29b-41d4-a716-446655443000");
        DeviceCommand command = command();
        when(deviceCommandQueryService.handle(org.mockito.ArgumentMatchers.any(GetLatestDeviceCommandByDeviceForUserQuery.class))).thenReturn(Optional.of(command));

        mockMvc.perform(get("/api/v1/devices/{deviceId}/commands/latest", command.getDeviceId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldReturnNotFoundWhenCommandDoesNotExist() throws Exception {
        authenticate("550e8400-e29b-41d4-a716-446655443000");
        when(deviceCommandQueryService.handle(org.mockito.ArgumentMatchers.any(GetDeviceCommandByIdForUserQuery.class))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/devices/{deviceId}/commands/{commandId}", UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    private DeviceCommand command() {
        Device device = new Device("SN-2000", "Sensor 2000", new HardwareId("CLAIR-0KBG"), ApiKey.generate(), new DeviceType("air-quality-v1"));
        org.springframework.test.util.ReflectionTestUtils.setField(device, "id", UUID.fromString("550e8400-e29b-41d4-a716-446655443001"));
        DeviceCommand command = new DeviceCommand(device.getId(), DeviceCommandType.WAKE, "{}");
        org.springframework.test.util.ReflectionTestUtils.setField(command, "id", UUID.fromString("550e8400-e29b-41d4-a716-446655443002"));
        return command;
    }

    private void authenticate(String userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new User(userId, "N/A", Collections.emptyList()), null, Collections.emptyList())
        );
    }
}
