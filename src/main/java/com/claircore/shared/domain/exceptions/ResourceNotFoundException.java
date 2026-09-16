package com.claircore.shared.domain.exceptions;

/**
 * Raised when a requested resource does not exist or is no longer available.
 * Transport adapters decide how to represent this failure.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
