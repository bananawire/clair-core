package com.claircore.iam.application.commandservices;

import com.claircore.iam.domain.model.commands.AuthenticateWithGoogleCommand;
import com.claircore.iam.domain.model.aggregates.User;

import java.util.Optional;

public interface GoogleAuthenticationCommandService {
    Optional<User> handle(AuthenticateWithGoogleCommand command);
}
