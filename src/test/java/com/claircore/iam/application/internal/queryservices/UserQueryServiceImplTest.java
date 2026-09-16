package com.claircore.iam.application.internal.queryservices;

import com.claircore.iam.domain.model.aggregates.User;
import com.claircore.iam.domain.model.queries.GetUserByEmailQuery;
import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.Password;
import com.claircore.iam.domain.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserQueryServiceImpl service;

    @Test
    void shouldReturnActiveUserWhenUserExistsAndIsActive() {
        User user = new User(new EmailAddress("user@example.com"), new Password("encoded-password"));
        user.activate();
        when(userRepository.findByEmail(new EmailAddress("user@example.com"))).thenReturn(java.util.Optional.of(user));

        var result = service.handle(new GetUserByEmailQuery(new EmailAddress("user@example.com")));

        assertTrue(result.isPresent());
    }

    @Test
    void shouldReturnEmptyWhenUserDoesNotExist() {
        when(userRepository.findByEmail(new EmailAddress("missing@example.com"))).thenReturn(java.util.Optional.empty());

        var result = service.handle(new GetUserByEmailQuery(new EmailAddress("missing@example.com")));

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenUserIsNotActive() {
        User user = new User(new EmailAddress("user@example.com"), new Password("encoded-password"));
        when(userRepository.findByEmail(new EmailAddress("user@example.com"))).thenReturn(java.util.Optional.of(user));

        var result = service.handle(new GetUserByEmailQuery(new EmailAddress("user@example.com")));

        assertTrue(result.isEmpty());
    }
}
