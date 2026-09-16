package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.domain.model.aggregates.Organization;
import com.claircore.device.domain.model.commands.CreateOrganizationCommand;
import com.claircore.device.domain.model.queries.GetOrganizationByIdForUserQuery;
import com.claircore.device.domain.model.queries.GetOrganizationsByOwnerQuery;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.application.queryservices.DeviceQueryService;
import com.claircore.device.application.commandservices.OrganizationCommandService;
import com.claircore.device.interfaces.rest.resources.CreateOrganizationRequest;
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
import java.util.List;
import java.util.Optional;
import java.time.Instant;
import java.util.UUID;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrganizationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class OrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrganizationCommandService organizationCommandService;

    @MockitoBean
    private DeviceQueryService deviceQueryService;

    @MockitoBean
    private TokenQueryService tokenQueryService;

    @BeforeEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateOrganizationWhenRequestIsValid() throws Exception {
        authenticate("550e8400-e29b-41d4-a716-446655446000");
        Organization organization = Organization.reconstitute(UUID.randomUUID(), "Home", new UserId(UUID.fromString("550e8400-e29b-41d4-a716-446655446000")), Instant.now(), Instant.now());
        when(organizationCommandService.handle(org.mockito.ArgumentMatchers.any(CreateOrganizationCommand.class))).thenReturn(organization);

        mockMvc.perform(post("/api/v1/organizations")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateOrganizationRequest("Home"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Home"));
    }

    @Test
    void shouldReturnForbiddenWhenCreatingOrganizationWithoutAuthentication() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(post("/api/v1/organizations")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateOrganizationRequest("Home"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnNotFoundWhenOrganizationDoesNotExist() throws Exception {
        authenticate("550e8400-e29b-41d4-a716-446655446000");
        when(deviceQueryService.handle(org.mockito.ArgumentMatchers.any(GetOrganizationByIdForUserQuery.class))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/organizations/{organizationId}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnOrganizationsWhenUserHasAny() throws Exception {
        authenticate("550e8400-e29b-41d4-a716-446655446000");
        Organization organization = Organization.reconstitute(UUID.randomUUID(), "Home", new UserId(UUID.fromString("550e8400-e29b-41d4-a716-446655446000")), Instant.now(), Instant.now());
        when(deviceQueryService.handle(org.mockito.ArgumentMatchers.any(GetOrganizationsByOwnerQuery.class))).thenReturn(List.of(organization));

        mockMvc.perform(get("/api/v1/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Home"));
    }

    @Test
    void readByIdIsRefusedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/{organizationId}", UUID.randomUUID())).andExpect(status().isForbidden());
        org.mockito.Mockito.verifyNoInteractions(deviceQueryService);
    }

    private void authenticate(String userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new User(userId, "N/A", Collections.emptyList()), null, Collections.emptyList())
        );
    }
}
