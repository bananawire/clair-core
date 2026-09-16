package com.claircore;

import com.claircore.analytics.application.internal.schedulers.AnalyticsScheduler;
import com.claircore.iam.application.commandservices.GoogleAuthenticationCommandService;
import com.claircore.iam.application.internal.outboundservices.oauth.GoogleTokenVerifier;
import com.claircore.iam.domain.model.aggregates.User;
import com.claircore.iam.domain.model.commands.AuthenticateWithGoogleCommand;
import com.claircore.iam.domain.model.valueobjects.*;
import com.claircore.iam.domain.repositories.UserRepository;
import com.claircore.notifications.application.internal.outboundservices.email.EmailDeliveryService;
import com.claircore.notifications.application.internal.outboundservices.push.PushNotificationDeliveryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** Loads every context together, with only external delivery and scheduled jobs substituted. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:application-smoke;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "jwt.secret=test-secret-that-is-at-least-thirty-two-characters-long",
        "google.oauth.client-id=test-client", "google.oauth.client-secret=test-secret",
        "google.oauth.redirect-uri=http://localhost/callback", "frontend.url=http://localhost",
        "stripe.api.key=sk_test_placeholder", "stripe.public.key=pk_test_placeholder", "stripe.webhook.secret=whsec_test",
        "spring.mail.host=localhost", "spring.mail.port=2525", "spring.mail.username=test", "spring.mail.password=test",
        "spring.cache.type=simple", "spring.data.redis.repositories.enabled=false"
})
class ApplicationContextIntegrationTest {
    @Autowired GoogleAuthenticationCommandService google;
    @Autowired UserRepository users;
    @MockitoBean GoogleTokenVerifier verifier;
    @MockitoBean EmailDeliveryService email;
    @MockitoBean PushNotificationDeliveryService push;
    @MockitoBean AnalyticsScheduler scheduler;

    @Test void existingGoogleLoginChangesSurviveReloadingFromTheDatabase() {
        var address = new EmailAddress("existing-google@example.com");
        users.save(new User(address, new Password("encoded-password")));
        var token = new GoogleIdToken("test-token");
        when(verifier.verify(token)).thenReturn(Optional.of(new VerifiedGoogleIdentity(
                address, new GoogleUserId("google-subject"), true)));

        assertThat(google.handle(new AuthenticateWithGoogleCommand(token))).isPresent();

        var stored = users.findByEmail(address).orElseThrow();
        assertThat(stored.isActive()).isTrue();
        assertThat(stored.getOauthProvider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(stored.getOauthSubject()).isEqualTo("google-subject");
    }
}
