package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.domain.model.commands.CreateSpaceCommand;
import com.claircore.device.domain.model.commands.DeleteSpaceCommand;
import com.claircore.device.domain.model.commands.UpdateSpaceNameCommand;
import com.claircore.device.domain.model.aggregates.Space;
import com.claircore.device.domain.model.queries.GetSpaceByIdForUserQuery;
import com.claircore.device.domain.model.queries.GetSpacesByOrganizationForUserQuery;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.application.commandservices.SpaceCommandService;
import com.claircore.device.application.queryservices.DeviceQueryService;
import com.claircore.device.interfaces.rest.resources.CreateSpaceRequest;
import com.claircore.device.interfaces.rest.resources.SpaceResponse;
import com.claircore.device.interfaces.rest.resources.UpdateSpaceNameRequest;
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
@RequestMapping("/api/v1/spaces")
@Tag(name = "Spaces", description = "Space management endpoints")
public class SpaceController {

    private final SpaceCommandService spaceCommandService;
    private final DeviceQueryService deviceQueryService;

    public SpaceController(SpaceCommandService spaceCommandService, DeviceQueryService deviceQueryService) {
        this.spaceCommandService = spaceCommandService;
        this.deviceQueryService = deviceQueryService;
    }

    @PostMapping
    @Operation(summary = "Create a new space")
    public ResponseEntity<SpaceResponse> createSpace(
            @RequestParam UUID organizationId,
            @RequestBody CreateSpaceRequest req) {

        UUID userId = getAuthenticatedUserId();
        var command = new CreateSpaceCommand(
            req.name(),
            organizationId,
            new UserId(userId)
        );

        Space space = spaceCommandService.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(space));
    }

    @GetMapping("/{spaceId}")
    @Operation(summary = "Get space by ID")
    public ResponseEntity<SpaceResponse> getSpace(@PathVariable UUID spaceId) {
        var query = new GetSpaceByIdForUserQuery(spaceId, new UserId(getAuthenticatedUserId()));
        return deviceQueryService.handle(query)
            .map(space -> ResponseEntity.ok(toResponse(space)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Get spaces by organization")
    public ResponseEntity<List<SpaceResponse>> getSpacesByOrganization(@RequestParam UUID organizationId) {
        var query = new GetSpacesByOrganizationForUserQuery(organizationId, new UserId(getAuthenticatedUserId()));
        List<Space> spaces = deviceQueryService.handle(query);
        return ResponseEntity.ok(spaces.stream().map(this::toResponse).toList());
    }

    @DeleteMapping("/{spaceId}")
    @Operation(summary = "Delete a space")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Space deleted"),
        @ApiResponse(responseCode = "400", description = "Space not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Space has registered devices", content = @Content)
    })
    public ResponseEntity<Void> deleteSpace(@PathVariable UUID spaceId) {
        spaceCommandService.handle(new DeleteSpaceCommand(spaceId, new UserId(getAuthenticatedUserId())));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping({"/{spaceId}/name", "/{spaceId}"})
    @Operation(summary = "Update space name")
    public ResponseEntity<Void> updateSpaceName(
            @PathVariable UUID spaceId,
            @RequestBody UpdateSpaceNameRequest request) {

        spaceCommandService.handle(new UpdateSpaceNameCommand(spaceId, request.name(), new UserId(getAuthenticatedUserId())));
        return ResponseEntity.ok().build();
    }

    private UUID getAuthenticatedUserId() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails userDetails)) {
            throw new org.springframework.security.access.AccessDeniedException("User not authenticated");
        }
        return UUID.fromString(userDetails.getUsername());
    }

    private SpaceResponse toResponse(Space space) {
        return new SpaceResponse(
            space.getId(),
            space.getName(),
            space.getOrganizationId(),
            space.getOwnerUserId().userId(),
            space.getCreatedAt(),
            space.getUpdatedAt()
        );
    }
}
