package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.domain.model.commands.CreateOrganizationCommand;
import com.claircore.device.domain.model.commands.DeleteOrganizationCommand;
import com.claircore.device.domain.model.commands.UpdateOrganizationNameCommand;
import com.claircore.device.domain.model.aggregates.Organization;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.application.commandservices.OrganizationCommandService;
import com.claircore.device.application.queryservices.DeviceQueryService;
import com.claircore.device.domain.model.queries.GetOrganizationByIdForUserQuery;
import com.claircore.device.domain.model.queries.GetOrganizationsByOwnerQuery;
import com.claircore.device.interfaces.rest.resources.CreateOrganizationRequest;
import com.claircore.device.interfaces.rest.resources.OrganizationResponse;
import com.claircore.device.interfaces.rest.resources.UpdateOrganizationNameRequest;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organizations", description = "Organization management endpoints")
public class OrganizationController {

    private final OrganizationCommandService organizationCommandService;
    private final DeviceQueryService deviceQueryService;

    public OrganizationController(
            OrganizationCommandService organizationCommandService,
            DeviceQueryService deviceQueryService) {
        this.organizationCommandService = organizationCommandService;
        this.deviceQueryService = deviceQueryService;
    }

    @PostMapping
    @Operation(summary = "Create a new organization")
    public ResponseEntity<OrganizationResponse> createOrganization(
            @RequestBody CreateOrganizationRequest req) {

        UUID userId = getAuthenticatedUserId();
        var command = new CreateOrganizationCommand(
            req.name(),
            new UserId(userId)
        );

        Organization org = organizationCommandService.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(org));
    }

    @GetMapping("/{organizationId}")
    @Operation(summary = "Get organization by ID")
    public ResponseEntity<OrganizationResponse> getOrganization(@PathVariable UUID organizationId) {
        var query = new GetOrganizationByIdForUserQuery(organizationId, new UserId(getAuthenticatedUserId()));
        return deviceQueryService.handle(query)
            .map(org -> ResponseEntity.ok(toResponse(org)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Get organizations for current user")
    public ResponseEntity<List<OrganizationResponse>> getUserOrganizations() {
        UUID userId = getAuthenticatedUserId();
        var query = new GetOrganizationsByOwnerQuery(new UserId(userId));
        List<Organization> orgs = deviceQueryService.handle(query);
        return ResponseEntity.ok(orgs.stream().map(this::toResponse).toList());
    }

    @DeleteMapping("/{organizationId}")
    @Operation(summary = "Delete organization")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Organization deleted"),
        @ApiResponse(responseCode = "400", description = "Organization not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Organization has registered devices", content = @Content)
    })
    public ResponseEntity<Void> deleteOrganization(@PathVariable UUID organizationId) {
        organizationCommandService.handle(new DeleteOrganizationCommand(organizationId, new UserId(getAuthenticatedUserId())));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping({"/{organizationId}/name", "/{organizationId}"})
    @Operation(summary = "Update organization name")
    public ResponseEntity<Void> updateOrganizationName(
            @PathVariable UUID organizationId,
            @RequestBody UpdateOrganizationNameRequest request) {

        organizationCommandService.handle(new UpdateOrganizationNameCommand(organizationId, request.name(), new UserId(getAuthenticatedUserId())));
        return ResponseEntity.ok().build();
    }

    private UUID getAuthenticatedUserId() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails userDetails)) {
            throw new org.springframework.security.access.AccessDeniedException("User not authenticated");
        }
        return UUID.fromString(userDetails.getUsername());
    }

    private OrganizationResponse toResponse(Organization org) {
        return new OrganizationResponse(
            org.getId(),
            org.getName(),
            org.getOwnerUserId().userId(),
            org.getCreatedAt(),
            org.getUpdatedAt()
        );
    }
}
