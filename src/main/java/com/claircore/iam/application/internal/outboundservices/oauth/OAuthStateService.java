package com.claircore.iam.application.internal.outboundservices.oauth;

/**
 * Outbound port for the OAuth {@code state} parameter — the value that ties a callback back to the
 * authorize request this server issued, and the only thing standing between the callback and CSRF.
 */
public interface OAuthStateService {

    String generateState();

    boolean validateState(String state);
}
