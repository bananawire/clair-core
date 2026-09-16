package com.claircore.iam.application.internal.outboundservices.oauth;

import java.util.Optional;

/**
 * Outbound port for the authorization-code half of the Google flow: trade a one-time code for an
 * ID token. Empty on any failure — the caller must not learn why, since the code came from a
 * redirect it does not control.
 */
public interface GoogleTokenExchange {

    Optional<String> exchangeCodeForIdToken(String code, String clientId, String clientSecret, String redirectUri);
}
