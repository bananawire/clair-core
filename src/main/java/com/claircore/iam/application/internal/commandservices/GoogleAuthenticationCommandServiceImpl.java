package com.claircore.iam.application.internal.commandservices;

import com.claircore.iam.domain.model.commands.AuthenticateWithGoogleCommand;
import com.claircore.iam.application.commandservices.GoogleAuthenticationCommandService;
import com.claircore.iam.application.internal.outboundservices.oauth.GoogleTokenVerifier;
import com.claircore.iam.domain.model.aggregates.User;
import com.claircore.iam.domain.model.events.UserAuthenticatedWithGoogleEvent;
import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.OAuthProvider;
import com.claircore.iam.domain.repositories.UserRepository;
import com.claircore.iam.interfaces.events.UserRegisteredIntegrationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class GoogleAuthenticationCommandServiceImpl implements GoogleAuthenticationCommandService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public GoogleAuthenticationCommandServiceImpl(
            GoogleTokenVerifier googleTokenVerifier,
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Optional<User> handle(AuthenticateWithGoogleCommand command) {
        var verifiedIdentity = googleTokenVerifier.verify(command.idToken());
        if (verifiedIdentity.isEmpty()) {
            return Optional.empty();
        }

        EmailAddress email = verifiedIdentity.get().email();
        String subject = verifiedIdentity.get().userId().subject();

        var existingUser = userRepository.findByEmail(email);

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            if (!user.isActive()) {
                user.activate();
            }
            if (!user.isOAuthUser()) {
                user.linkOAuthAccount(OAuthProvider.GOOGLE, subject);
            }
            user = userRepository.save(user);
        } else {
            user = new User(email, OAuthProvider.GOOGLE, subject);
            user = userRepository.save(user);
            eventPublisher.publishEvent(new UserRegisteredIntegrationEvent(user.getId()));
        }

        eventPublisher.publishEvent(new UserAuthenticatedWithGoogleEvent(
                user.getId(),
                user.getEmail(),
                OAuthProvider.GOOGLE
        ));

        return Optional.of(user);
    }
}
