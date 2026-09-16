package com.claircore.iam.application.internal.outboundservices.oauth;

import com.claircore.iam.domain.model.valueobjects.GoogleIdToken;
import com.claircore.iam.domain.model.valueobjects.VerifiedGoogleIdentity;

import java.util.Optional;

/**
 * Outbound port for checking a Google ID token and reading the identity out of it. Empty means the
 * token is not trustworthy — wrong issuer, wrong audience, expired, or an unverified email.
 */
public interface GoogleTokenVerifier {

    Optional<VerifiedGoogleIdentity> verify(GoogleIdToken idToken);
}
