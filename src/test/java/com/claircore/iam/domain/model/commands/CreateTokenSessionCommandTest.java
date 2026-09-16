package com.claircore.iam.domain.model.commands;

import com.claircore.iam.domain.model.aggregates.User;
import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.Password;
import com.claircore.iam.domain.model.valueobjects.TokenType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class CreateTokenSessionCommandTest {

    @Test
    void shouldCreateTokenSessionCommandWhenValuesAreValid() {
        User user = new User(new EmailAddress("user@example.com"), new Password("encoded-password"));

        CreateTokenSessionCommand command = new CreateTokenSessionCommand(user, TokenType.ACCESS, 1000L);

        assertEquals(user, command.user());
        assertEquals(TokenType.ACCESS, command.type());
        assertEquals(1000L, command.ttlMillis());
    }

    @Test
    void shouldThrowExceptionWhenUserIsMissing() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new CreateTokenSessionCommand(null, TokenType.ACCESS, 1000L)
        );

        assertEquals("User is required", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenTokenTypeIsMissing() {
        User user = new User(new EmailAddress("user@example.com"), new Password("encoded-password"));

        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new CreateTokenSessionCommand(user, null, 1000L)
        );

        assertEquals("Token type is required", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenTtlIsNotPositive() {
        User user = new User(new EmailAddress("user@example.com"), new Password("encoded-password"));

        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new CreateTokenSessionCommand(user, TokenType.ACCESS, 0L)
        );

        assertEquals("TTL must be positive", exception.getMessage());
    }
}
