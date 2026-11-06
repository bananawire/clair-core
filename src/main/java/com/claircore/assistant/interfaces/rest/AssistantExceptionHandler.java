package com.claircore.assistant.interfaces.rest;

import com.claircore.assistant.domain.exceptions.AssistantQuotaExceededException;
import com.claircore.assistant.domain.exceptions.AssistantUnavailableException;
import com.claircore.assistant.interfaces.rest.controllers.AssistantController;
import com.claircore.shared.interfaces.rest.resources.ErrorResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Duration;
import java.time.Instant;

/**
 * Maps the assistant's own failures. It lives in this context because {@code shared} must not
 * depend on any bounded context, and runs before the global handler so these exceptions do not
 * fall through to its generic 500. Bodies use the same {@link ErrorResource}.
 */
@RestControllerAdvice(assignableTypes = AssistantController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AssistantExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(AssistantExceptionHandler.class);

    @ExceptionHandler(AssistantQuotaExceededException.class)
    public ResponseEntity<ErrorResource> handleQuotaExceeded(AssistantQuotaExceededException ex) {
        HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;
        var response = ResponseEntity.status(status);
        if (ex.getUsage() != null) {
            long seconds = Math.max(0, Duration.between(Instant.now(), ex.getUsage().resetsAt()).getSeconds());
            response.header(HttpHeaders.RETRY_AFTER, Long.toString(seconds));
        }
        return response.body(ErrorResource.of(status.value(), status.getReasonPhrase(), ex.getMessage()));
    }

    @ExceptionHandler(AssistantUnavailableException.class)
    public ResponseEntity<ErrorResource> handleUnavailable(AssistantUnavailableException ex) {
        logger.warn("Assistant unavailable: {}", ex.getMessage());
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status)
                .body(ErrorResource.of(status.value(), status.getReasonPhrase(), ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResource> handleUnreadableBody(HttpMessageNotReadableException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(ErrorResource.of(status.value(), status.getReasonPhrase(), "Malformed request body"));
    }
}
