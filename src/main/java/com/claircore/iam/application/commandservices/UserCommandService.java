package com.claircore.iam.application.commandservices;

import com.claircore.iam.domain.model.commands.ConfirmRegistrationCommand;
import com.claircore.iam.domain.model.commands.InitiateRegistrationCommand;
import com.claircore.iam.domain.model.aggregates.RegistrationSession;
import com.claircore.iam.domain.model.aggregates.User;

import java.util.Optional;

public interface UserCommandService {
    Optional<RegistrationSession> handle(InitiateRegistrationCommand command);
    Optional<User> handle(ConfirmRegistrationCommand command);
}
