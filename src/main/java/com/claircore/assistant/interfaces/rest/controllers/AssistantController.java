package com.claircore.assistant.interfaces.rest.controllers;

import com.claircore.assistant.application.commandservices.AssistantCommandService;
import com.claircore.assistant.application.queryservices.AssistantQueryService;
import com.claircore.assistant.domain.model.commands.RateAssistantMessageCommand;
import com.claircore.assistant.domain.model.queries.GetAssistantUsageQuery;
import com.claircore.assistant.domain.model.queries.GetConversationMessagesQuery;
import com.claircore.assistant.domain.model.queries.GetConversationsByUserQuery;
import com.claircore.assistant.domain.model.valueobjects.FeedbackRating;
import com.claircore.assistant.interfaces.rest.resources.AssistantReplyResource;
import com.claircore.assistant.interfaces.rest.resources.AssistantUsageResource;
import com.claircore.assistant.interfaces.rest.resources.ConversationMessageResource;
import com.claircore.assistant.interfaces.rest.resources.ConversationResource;
import com.claircore.assistant.interfaces.rest.resources.RateAssistantMessageRequest;
import com.claircore.assistant.interfaces.rest.resources.SendAssistantMessageRequest;
import com.claircore.assistant.interfaces.rest.transform.AssistantResourceFromEntityAssembler;
import com.claircore.assistant.interfaces.rest.transform.SendAssistantMessageCommandFromResourceAssembler;
import com.claircore.shared.interfaces.rest.security.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assistant")
@Tag(name = "Assistant", description = "AI assistant that answers questions about the user's air quality data")
public class AssistantController {

    private static final int MAX_PAGE_SIZE = 50;

    private final AssistantCommandService assistantCommandService;
    private final AssistantQueryService assistantQueryService;

    public AssistantController(AssistantCommandService assistantCommandService, AssistantQueryService assistantQueryService) {
        this.assistantCommandService = assistantCommandService;
        this.assistantQueryService = assistantQueryService;
    }

    @PostMapping("/messages")
    @Operation(summary = "Ask the assistant a question, in a new or an existing conversation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Answer returned and both turns stored"),
            @ApiResponse(responseCode = "400", description = "Invalid question"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Conversation not found"),
            @ApiResponse(responseCode = "429", description = "Daily message limit of the plan reached"),
            @ApiResponse(responseCode = "503", description = "Assistant not configured or temporarily unavailable")
    })
    public ResponseEntity<AssistantReplyResource> sendMessage(
            @CurrentUserId UUID userId,
            @Valid @RequestBody SendAssistantMessageRequest request) {
        var command = SendAssistantMessageCommandFromResourceAssembler.toCommandFromResource(userId, request);
        var reply = assistantCommandService.handle(command);
        return ResponseEntity.ok(AssistantResourceFromEntityAssembler.toResourceFromEntity(reply));
    }

    @GetMapping("/conversations")
    @Operation(summary = "List the user's conversations, most recently active first")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conversations returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<Page<ConversationResource>> getConversations(
            @CurrentUserId UUID userId,
            @Parameter(description = "Page number (default: 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 20, max: 50)") @RequestParam(defaultValue = "20") int size) {
        var result = assistantQueryService.handle(
                new GetConversationsByUserQuery(userId, page, Math.min(size, MAX_PAGE_SIZE)));
        var resources = result.items().stream()
                .map(AssistantResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(new PageImpl<>(resources, PageRequest.of(result.page(), result.size()), result.total()));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Get every message of one of the user's conversations, oldest first")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Messages returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Conversation not found")
    })
    public ResponseEntity<List<ConversationMessageResource>> getConversationMessages(
            @CurrentUserId UUID userId,
            @PathVariable UUID conversationId) {
        var messages = assistantQueryService.handle(new GetConversationMessagesQuery(userId, conversationId));
        return ResponseEntity.ok(messages.stream()
                .map(AssistantResourceFromEntityAssembler::toResourceFromEntity)
                .toList());
    }

    @GetMapping("/usage")
    @Operation(summary = "Get today's assistant usage against the plan's daily allowance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usage returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<AssistantUsageResource> getUsage(@CurrentUserId UUID userId) {
        var usage = assistantQueryService.handle(new GetAssistantUsageQuery(userId));
        return ResponseEntity.ok(AssistantResourceFromEntityAssembler.toResourceFromEntity(usage));
    }

    @PutMapping("/messages/{messageId}/feedback")
    @Operation(summary = "Rate one of the assistant's answers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Rating stored"),
            @ApiResponse(responseCode = "400", description = "Invalid rating"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Assistant message not found")
    })
    public ResponseEntity<Void> rateMessage(
            @CurrentUserId UUID userId,
            @PathVariable UUID messageId,
            @Valid @RequestBody RateAssistantMessageRequest request) {
        assistantCommandService.handle(
                new RateAssistantMessageCommand(userId, messageId, FeedbackRating.valueOf(request.rating())));
        return ResponseEntity.noContent().build();
    }
}
