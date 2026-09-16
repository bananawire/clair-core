package com.claircore.shared.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Error payload returned by the global exception handler.
 *
 * <p>The timestamp is pinned to an ISO-8601 string: the handler used to emit a pre-formatted
 * {@code Instant.now().toString()}, and without the annotation the wire format would depend on the
 * global {@code write-dates-as-timestamps} setting.
 *
 * @param timestamp when the error was produced
 * @param status    HTTP status code
 * @param error     HTTP reason phrase, or a short error label
 * @param message   human readable description, absent for field-level validation failures
 * @param details   field name to validation message, absent for non-validation errors
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResource(
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> details) {

    public static ErrorResource of(int status, String error, String message) {
        return new ErrorResource(Instant.now(), status, error, message, null);
    }

    public static ErrorResource validation(int status, Map<String, String> details) {
        // Not Map.copyOf: a constraint declared without a message yields a null default message, and
        // rejecting it here would turn a field-level 400 into a 500 raised inside the handler.
        return new ErrorResource(
                Instant.now(),
                status,
                "Validation failed",
                null,
                Collections.unmodifiableMap(new LinkedHashMap<>(details)));
    }
}
