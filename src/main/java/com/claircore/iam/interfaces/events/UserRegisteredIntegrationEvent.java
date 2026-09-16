package com.claircore.iam.interfaces.events;

import java.util.UUID;

/**
 * The published contract for a newly registered account: the only iam event another context may
 * listen to. Billing consumes it to open the user's plan.
 */
public record UserRegisteredIntegrationEvent(UUID userId) {
    public UserRegisteredIntegrationEvent {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
    }
}
