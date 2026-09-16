package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.domain.model.aggregates.Space;
import com.claircore.device.domain.model.commands.CreateSpaceCommand;
import com.claircore.device.domain.model.queries.GetSpaceByIdForUserQuery;
import com.claircore.device.domain.model.queries.GetSpacesByOrganizationForUserQuery;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.application.queryservices.DeviceQueryService;
import com.claircore.device.application.commandservices.SpaceCommandService;
import com.claircore.device.interfaces.rest.resources.CreateSpaceRequest;
import com.claircore.iam.application.queryservices.TokenQueryService;
import com.claircore.shared.interfaces.rest.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.claircore.device.domain.model.commands.DeleteSpaceCommand;
import com.claircore.device.domain.model.commands.UpdateSpaceNameCommand;
import com.claircore.device.interfaces.rest.resources.UpdateSpaceNameRequest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SpaceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class SpaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SpaceCommandService spaceCommandService;

    @MockitoBean
    private DeviceQueryService deviceQueryService;

    @MockitoBean
    private TokenQueryService tokenQueryService;

    @BeforeEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateSpaceWhenRequestIsValid() throws Exception {
        authenticate("550e8400-e29b-41d4-a716-446655445000");
        Space space = Space.reconstitute(UUID.randomUUID(), "Kitchen", UUID.randomUUID(), new UserId(UUID.fromString("550e8400-e29b-41d4-a716-446655445000")), Instant.now(), Instant.now());
        when(spaceCommandService.handle(org.mockito.ArgumentMatchers.any(CreateSpaceCommand.class))).thenReturn(space);

        mockMvc.perform(post("/api/v1/spaces")
                        .param("organizationId", UUID.randomUUID().toString())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateSpaceRequest("Kitchen"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Kitchen"));
    }

    @Test
    void shouldReturnForbiddenWhenCreatingSpaceWithoutAuthentication() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(post("/api/v1/spaces")
                        .param("organizationId", UUID.randomUUID().toString())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateSpaceRequest("Kitchen"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnNotFoundWhenSpaceDoesNotExist() throws Exception {
        authenticate("550e8400-e29b-41d4-a716-446655445000");
        when(deviceQueryService.handle(org.mockito.ArgumentMatchers.any(GetSpaceByIdForUserQuery.class))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/spaces/{spaceId}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnSpacesWhenOrganizationHasSpaces() throws Exception {
        authenticate("550e8400-e29b-41d4-a716-446655445000");
        Space space = Space.reconstitute(UUID.randomUUID(), "Kitchen", UUID.randomUUID(), new UserId(UUID.fromString("550e8400-e29b-41d4-a716-446655445000")), Instant.now(), Instant.now());
        when(deviceQueryService.handle(org.mockito.ArgumentMatchers.any(GetSpacesByOrganizationForUserQuery.class))).thenReturn(List.of(space));

        mockMvc.perform(get("/api/v1/spaces").param("organizationId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Kitchen"));
    }

    @Test
    void readsAreRefusedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/spaces/{spaceId}", UUID.randomUUID())).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/spaces").param("organizationId", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
        org.mockito.Mockito.verifyNoInteractions(deviceQueryService);
    }

    @Test
    void deleteAndRenameCarryTheAuthenticatedActor() throws Exception {
        authenticate("550e8400-e29b-41d4-a716-446655445000");
        UUID spaceId = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/spaces/{spaceId}", spaceId)).andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/v1/spaces/{spaceId}/name", spaceId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new UpdateSpaceNameRequest("Bedroom"))))
                .andExpect(status().isOk());
        var deleteCaptor = org.mockito.ArgumentCaptor.forClass(DeleteSpaceCommand.class);
        org.mockito.Mockito.verify(spaceCommandService).handle(deleteCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(
                UUID.fromString("550e8400-e29b-41d4-a716-446655445000"), deleteCaptor.getValue().userId().userId());
        var renameCaptor = org.mockito.ArgumentCaptor.forClass(UpdateSpaceNameCommand.class);
        org.mockito.Mockito.verify(spaceCommandService).handle(renameCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("Bedroom", renameCaptor.getValue().name());
    }

    private void authenticate(String userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new User(userId, "N/A", Collections.emptyList()), null, Collections.emptyList())
        );
    }
}
