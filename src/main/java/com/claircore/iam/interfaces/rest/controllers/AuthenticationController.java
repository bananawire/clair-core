package com.claircore.iam.interfaces.rest.controllers;

import com.claircore.iam.application.internal.commandservices.GoogleOAuthCallbackApplicationService;
import com.claircore.iam.domain.model.commands.SignOutCommand;
import com.claircore.iam.domain.model.queries.GetUserByEmailQuery;
import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.UserId;
import com.claircore.iam.application.commandservices.GoogleAuthenticationCommandService;
import com.claircore.iam.application.commandservices.TokenCommandService;
import com.claircore.iam.application.commandservices.UserCommandService;
import com.claircore.iam.application.internal.outboundservices.oauth.OAuthStateService;
import com.claircore.iam.application.queryservices.TokenQueryService;
import com.claircore.iam.application.queryservices.UserQueryService;
import com.claircore.iam.interfaces.rest.resources.*;
import com.claircore.iam.interfaces.rest.transform.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping(value = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Authentication", description = "Authentication and Registration Endpoints")
public class AuthenticationController {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;
    private final TokenCommandService tokenCommandService;
    private final TokenQueryService tokenQueryService;
    private final GoogleAuthenticationCommandService googleAuthenticationCommandService;
    private final GoogleOAuthCallbackApplicationService googleOAuthCallbackApplicationService;
    private final OAuthStateService oAuthStateService;
    private final PasswordEncoder passwordEncoder;

    private final String googleClientId;
    private final String googleClientSecret;
    private final String googleRedirectUri;
    private final String frontendUrl;

    public AuthenticationController(
            UserCommandService userCommandService,
            UserQueryService userQueryService,
            TokenCommandService tokenCommandService,
            TokenQueryService tokenQueryService,
            GoogleAuthenticationCommandService googleAuthenticationCommandService,
            GoogleOAuthCallbackApplicationService googleOAuthCallbackApplicationService,
            OAuthStateService oAuthStateService,
            PasswordEncoder passwordEncoder,
            @Value("${google.oauth.client-id}") String googleClientId,
            @Value("${google.oauth.client-secret}") String googleClientSecret,
            @Value("${google.oauth.redirect-uri}") String googleRedirectUri,
            @Value("${frontend.url}") String frontendUrl
    ) {
        this.userCommandService = userCommandService;
        this.userQueryService = userQueryService;
        this.tokenCommandService = tokenCommandService;
        this.tokenQueryService = tokenQueryService;
        this.googleAuthenticationCommandService = googleAuthenticationCommandService;
        this.googleOAuthCallbackApplicationService = googleOAuthCallbackApplicationService;
        this.oAuthStateService = oAuthStateService;
        this.passwordEncoder = passwordEncoder;
        this.googleClientId = googleClientId;
        this.googleClientSecret = googleClientSecret;
        this.googleRedirectUri = googleRedirectUri;
        this.frontendUrl = frontendUrl;
    }

    @PostMapping("/sign-up")
    @Operation(summary = "Sign up a new user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Registration initiated, verification code sent"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<RegistrationInitiatedResource> signUp(@Valid @RequestBody InitiateRegistrationRequest request) {
        var command = InitiateRegistrationCommandFromRequestAssembler.toCommandFromRequest(request);
        var session = userCommandService.handle(command);
        if (session.isEmpty()) return ResponseEntity.badRequest().build();
        var resource = RegistrationInitiatedResourceFromSessionAssembler.toResourceFromSession(session.get());
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    @PostMapping("/confirm")
    @Operation(summary = "Confirm registration with verification code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Registration confirmed, user created"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired session/code")
    })
    public ResponseEntity<UserResource> confirm(@Valid @RequestBody ConfirmRegistrationRequest request) {
        var command = ConfirmRegistrationCommandFromRequestAssembler.toCommandFromRequest(request);
        var user = userCommandService.handle(command);
        if (user.isEmpty()) return ResponseEntity.badRequest().build();
        var resource = UserResourceFromEntityAssembler.toResourceFromEntity(user.get());
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    @PostMapping("/sign-in")
    @Operation(summary = "Sign in an existing verified user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials or user not verified")
    })
    public ResponseEntity<AuthenticatedUserResource> signIn(@Valid @RequestBody SignInRequest request) {
        var getUserByEmailQuery = new GetUserByEmailQuery(new EmailAddress(request.email()));
        var user = userQueryService.handle(getUserByEmailQuery);

        if (user.isEmpty() || !passwordEncoder.matches(request.password(), user.get().getPassword().passwordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var token = tokenCommandService.createAccessToken(user.get());
        var refreshToken = tokenCommandService.createRefreshToken(user.get());
        var authenticatedUserResource = new AuthenticatedUserResource(user.get().getId(), user.get().getEmail().address(), token, refreshToken);
        return ResponseEntity.ok(authenticatedUserResource);
    }

    @PostMapping("/google/sign-in")
    @Operation(summary = "Sign in or register using Google OAuth 2.0 ID token (direct flow)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful, returns JWT access and refresh tokens"),
            @ApiResponse(responseCode = "401", description = "Invalid or unverifiable Google ID token")
    })
    public ResponseEntity<AuthenticatedUserResource> googleSignIn(@Valid @RequestBody GoogleSignInRequest request) {
        var command = AuthenticateWithGoogleCommandFromRequestAssembler.toCommandFromRequest(request);
        var user = googleAuthenticationCommandService.handle(command);

        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var token = tokenCommandService.createAccessToken(user.get());
        var refreshToken = tokenCommandService.createRefreshToken(user.get());
        var authenticatedUserResource = new AuthenticatedUserResource(
                user.get().getId(),
                user.get().getEmail().address(),
                token,
                refreshToken
        );
        return ResponseEntity.ok(authenticatedUserResource);
    }

    @GetMapping("/google/authorize")
    @Operation(summary = "Initiate Google OAuth 2.0 authorization code flow")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Redirects to Google OAuth consent screen")
    })
    public ResponseEntity<Void> googleAuthorize() {
        String state = oAuthStateService.generateState();

        String googleAuthUrl = UriComponentsBuilder
                .fromHttpUrl("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", googleClientId)
                .queryParam("redirect_uri", googleRedirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("state", state)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .toUriString();

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(googleAuthUrl))
                .build();
    }

    @GetMapping("/google/callback")
    @Operation(summary = "Google OAuth 2.0 callback. Exchanges code for tokens and redirects to frontend.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Redirects to frontend with JWT tokens"),
            @ApiResponse(responseCode = "302", description = "Redirects to frontend error page on failure")
    })
    public ResponseEntity<Void> googleCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            @RequestParam(value = "error", required = false) String error) {

        if (error != null || !oAuthStateService.validateState(state)) {
            return redirectToFrontendError();
        }

        var user = googleOAuthCallbackApplicationService.handle(code, googleClientId, googleClientSecret, googleRedirectUri);

        if (user.isEmpty()) {
            return redirectToFrontendError();
        }

        var accessToken = tokenCommandService.createAccessToken(user.get());
        var refreshToken = tokenCommandService.createRefreshToken(user.get());

        String redirectUrl = UriComponentsBuilder.fromHttpUrl(frontendUrl + "/auth/callback")
                .queryParam("token", accessToken)
                .queryParam("refreshToken", refreshToken)
                .toUriString();

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }

    @DeleteMapping("/sign-out")
    @Operation(summary = "Sign out user and revoke all active tokens")
    @SecurityRequirement(name = "BearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Signed out successfully, all tokens revoked"),
            @ApiResponse(responseCode = "401", description = "Invalid or missing token")
    })
    public ResponseEntity<Void> signOut(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.substring(7);
        if (!tokenQueryService.isAccessTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var userId = tokenQueryService.getUserIdFromToken(token);
        if (userId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        tokenCommandService.signOut(new SignOutCommand(new UserId(userId.get())));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using a valid refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "New access token generated"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<AuthenticatedUserResource> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        if (!tokenQueryService.isRefreshTokenValid(request.refreshToken())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var session = tokenQueryService.getTokenSession(request.refreshToken());
        var user = session
                .map(tokenSession -> userQueryService.handle(new GetUserByEmailQuery(tokenSession.email())))
                .orElseGet(Optional::empty);

        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var newRefreshToken = tokenCommandService.rotateRefreshToken(request.refreshToken())
                .orElse(null);
        if (newRefreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var newToken = tokenCommandService.createAccessToken(user.get());
        var resource = new AuthenticatedUserResource(user.get().getId(), user.get().getEmail().address(), newToken, newRefreshToken);
        return ResponseEntity.ok(resource);
    }

    @GetMapping("/verify")
    @Operation(summary = "Verify if an access token is valid and return its metadata")
    @SecurityRequirement(name = "BearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token is valid"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token")
    })
    public ResponseEntity<TokenVerificationResource> verifyToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new TokenVerificationResource(false, null, null));
        }

        String token = authHeader.substring(7);

        if (!tokenQueryService.isAccessTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new TokenVerificationResource(false, null, null));
        }

        var session = tokenQueryService.getTokenSession(token);
        var userId = tokenQueryService.getUserIdFromToken(token);
        var expiresAt = session.map(s -> s.expiresAt().toString()).orElse(null);
        return ResponseEntity.ok(new TokenVerificationResource(true, userId.orElse(null), expiresAt));
    }

    private ResponseEntity<Void> redirectToFrontendError() {
        String errorUrl = UriComponentsBuilder.fromHttpUrl(frontendUrl + "/auth/error")
                .queryParam("reason", "google_oauth_failed")
                .toUriString();
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(errorUrl))
                .build();
    }
}
