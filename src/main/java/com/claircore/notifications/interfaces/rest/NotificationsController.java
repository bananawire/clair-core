package com.claircore.notifications.interfaces.rest;

import com.claircore.shared.interfaces.rest.security.CurrentUserId;
import com.claircore.notifications.application.queryservices.PushNotificationHistoryQueryService;
import com.claircore.notifications.domain.model.queries.GetPushNotificationHistoryQuery;
import com.claircore.notifications.interfaces.rest.resources.PushNotificationResource;
import com.claircore.notifications.interfaces.rest.transform.PushNotificationResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Notifications", description = "Push notifications management endpoints")
public class NotificationsController {

    private static final int PAGE_SIZE = 20;

    private final PushNotificationHistoryQueryService pushNotificationHistoryQueryService;

    public NotificationsController(PushNotificationHistoryQueryService pushNotificationHistoryQueryService) {
        this.pushNotificationHistoryQueryService = pushNotificationHistoryQueryService;
    }

    @GetMapping("/notifications/push")
    @Operation(summary = "Get push notification logs for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notifications returned successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<Page<PushNotificationResource>> getUserNotifications(
            @CurrentUserId UUID userId,
            @Parameter(description = "Page number (default: 0)") @RequestParam(defaultValue = "0") Integer page) {

        var result = pushNotificationHistoryQueryService.handle(new GetPushNotificationHistoryQuery(userId, page, PAGE_SIZE));
        var resources = result.items().stream()
                .map(PushNotificationResourceFromEntityAssembler::toResourceFromEntity)
                .toList();

        // The port speaks PageResult; the response body stays a Spring Data page so the JSON
        // envelope clients already consume (content, totalElements, ...) is unchanged.
        return ResponseEntity.ok(new PageImpl<>(resources, PageRequest.of(result.page(), result.size()), result.total()));
    }
}
