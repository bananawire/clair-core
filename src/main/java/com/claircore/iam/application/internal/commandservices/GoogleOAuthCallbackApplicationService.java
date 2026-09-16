package com.claircore.iam.application.internal.commandservices;

import com.claircore.iam.domain.model.commands.AuthenticateWithGoogleCommand;
import com.claircore.iam.application.commandservices.GoogleAuthenticationCommandService;
import com.claircore.iam.application.internal.outboundservices.oauth.GoogleTokenExchange;
import com.claircore.iam.domain.model.aggregates.User;
import com.claircore.iam.domain.model.valueobjects.GoogleIdToken;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GoogleOAuthCallbackApplicationService {

    private final GoogleTokenExchange tokenClient;
    private final GoogleAuthenticationCommandService authenticationCommandService;

    public GoogleOAuthCallbackApplicationService(
            GoogleTokenExchange tokenClient,
            GoogleAuthenticationCommandService authenticationCommandService
    ) {
        this.tokenClient = tokenClient;
        this.authenticationCommandService = authenticationCommandService;
    }

    public Optional<User> handle(String code, String clientId, String clientSecret, String redirectUri) {
        var idTokenOpt = tokenClient.exchangeCodeForIdToken(code, clientId, clientSecret, redirectUri);
        if (idTokenOpt.isEmpty()) {
            return Optional.empty();
        }

        var command = new AuthenticateWithGoogleCommand(new GoogleIdToken(idTokenOpt.get()));
        return authenticationCommandService.handle(command);
    }
}
