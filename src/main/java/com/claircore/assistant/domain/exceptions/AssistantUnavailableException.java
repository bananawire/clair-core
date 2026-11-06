package com.claircore.assistant.domain.exceptions;

/**
 * Raised when the language model cannot produce an answer: it is not configured, it is down, or it
 * replied with nothing usable. Nothing is stored and no quota is consumed. Transport adapters
 * decide how to represent this failure.
 */
public class AssistantUnavailableException extends RuntimeException {

    public AssistantUnavailableException(String message) {
        super(message);
    }

    public AssistantUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
