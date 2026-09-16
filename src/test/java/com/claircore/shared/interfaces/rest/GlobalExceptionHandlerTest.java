package com.claircore.shared.interfaces.rest;

import com.claircore.shared.domain.exceptions.ResourceNotFoundException;
import com.claircore.shared.interfaces.rest.resources.ErrorResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /** Mirrors the Boot-configured web ObjectMapper: ISO-8601 timestamps, not epoch numbers. */
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @Test
    void shouldMapIllegalArgumentToBadRequest() {
        assertEquals(HttpStatus.BAD_REQUEST.value(), handler.handleIllegalArgument(new IllegalArgumentException("bad")).getStatusCode().value());
    }

    @Test
    void shouldMapIllegalStateToConflict() {
        assertEquals(HttpStatus.CONFLICT.value(), handler.handleIllegalState(new IllegalStateException("conflict")).getStatusCode().value());
    }

    @Test
    void shouldMapAccessDeniedToForbidden() {
        assertEquals(HttpStatus.FORBIDDEN.value(), handler.handleAccessDenied(new AccessDeniedException("forbidden")).getStatusCode().value());
    }

    @Test
    void shouldMapResourceNotFoundToNotFound() {
        assertEquals(HttpStatus.NOT_FOUND.value(), handler.handleResourceNotFound(new ResourceNotFoundException("missing")).getStatusCode().value());
    }

    @Test
    void shouldSerializeErrorBodyWithTimestampStatusErrorMessageAndNoDetails() throws Exception {
        var body = handler.handleIllegalArgument(new IllegalArgumentException("bad")).getBody();

        var json = objectMapper.readTree(objectMapper.writeValueAsString(body));

        assertEquals(400, json.get("status").asInt());
        assertEquals("Bad Request", json.get("error").asText());
        assertEquals("bad", json.get("message").asText());
        assertTrue(json.get("timestamp").isTextual(), "timestamp must stay an ISO-8601 string on the wire");
        assertFalse(json.has("details"), "details is absent for non-validation errors");
    }

    @Test
    void shouldSerializeValidationBodyWithDetailsAndNoMessage() throws Exception {
        var exception = validationException("email", "must not be blank");

        var response = handler.handleValidation(exception);
        var json = objectMapper.readTree(objectMapper.writeValueAsString(response.getBody()));

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertEquals(400, json.get("status").asInt());
        assertEquals("Validation failed", json.get("error").asText());
        assertEquals("must not be blank", json.get("details").get("email").asText());
        assertTrue(json.get("timestamp").isTextual());
        assertFalse(json.has("message"), "message is absent for field-level validation failures");
    }

    @Test
    void shouldStillReturnBadRequestWhenAConstraintHasNoDefaultMessage() {
        var exception = validationException("email", null);

        var response = handler.handleValidation(exception);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertNull(response.getBody().details().get("email"));
    }

    @Test
    void validationDetailsAreUnmodifiable() {
        var resource = ErrorResource.validation(400, Map.of("email", "must not be blank"));

        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> resource.details().put("other", "value"));
    }

    private MethodArgumentNotValidException validationException(String field, String message) {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Payload(), "payload");
        bindingResult.rejectValue(field, "invalid", message);
        MethodParameter parameter;
        try {
            parameter = new MethodParameter(GlobalExceptionHandlerTest.class.getDeclaredMethod("dummy", String.class), 0);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
        return new MethodArgumentNotValidException(parameter, bindingResult);
    }

    @SuppressWarnings("unused")
    private void dummy(String value) {
        // Target of the MethodParameter the exception requires; never invoked.
    }

    /** Binding target for the validation errors; needs a readable property. */
    static class Payload {
        private String email;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
