package com.claircore.iam.infrastructure.persistence.jpa.adapters;

import com.claircore.iam.domain.model.aggregates.User;
import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.OAuthProvider;
import com.claircore.iam.domain.model.valueobjects.Password;
import com.claircore.iam.domain.model.valueobjects.UserStatus;
import com.claircore.iam.domain.repositories.UserRepository;
import com.claircore.shared.infrastructure.config.JpaAuditingConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JpaAuditingConfiguration.class, UserRepositoryImpl.class})
class UserRepositoryImplTest {

    @Autowired
    private UserRepository repository;

    @Test
    void savesWithTheIdentityTheAggregateAssignedAndFillsTheAuditTimestamps() {
        var user = new User(new EmailAddress("user@example.com"), new Password("$2a$10$hash"));

        var saved = repository.save(user);

        assertThat(saved.getId()).isEqualTo(user.getId());
        assertThat(saved.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void roundTripsEveryFieldThroughStorage() {
        repository.save(new User(new EmailAddress("oauth@example.com"), OAuthProvider.GOOGLE, "google-subject-1"));

        var found = repository.findByEmail(new EmailAddress("oauth@example.com")).orElseThrow();

        assertThat(found.getEmail()).isEqualTo(new EmailAddress("oauth@example.com"));
        assertThat(found.getPassword()).isNotNull();
        assertThat(found.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(found.getOauthProvider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(found.getOauthSubject()).isEqualTo("google-subject-1");
        assertThat(found.isOAuthUser()).isTrue();
    }

    @Test
    void findsNothingForAnEmailThatWasNeverRegistered() {
        assertThat(repository.findByEmail(new EmailAddress("absent@example.com"))).isEmpty();
        assertThat(repository.existsByEmail(new EmailAddress("absent@example.com"))).isFalse();
    }

    @Test
    void existsByEmailIsWhatGuardsAgainstADuplicateRegistration() {
        repository.save(new User(new EmailAddress("taken@example.com"), new Password("$2a$10$hash")));

        assertThat(repository.existsByEmail(new EmailAddress("taken@example.com"))).isTrue();
    }

    @Test
    void reSavingAnActivatedUserUpdatesTheRowRatherThanInsertingASecond() {
        var user = repository.save(new User(new EmailAddress("pending@example.com"), new Password("$2a$10$hash")));

        user.activate();
        var reSaved = repository.save(user);

        assertThat(reSaved.getId()).isEqualTo(user.getId());
        assertThat(repository.findByEmail(new EmailAddress("pending@example.com")).orElseThrow().getStatus())
                .isEqualTo(UserStatus.ACTIVE);
    }
}
