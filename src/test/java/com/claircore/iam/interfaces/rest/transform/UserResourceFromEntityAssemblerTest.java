package com.claircore.iam.interfaces.rest.transform;

import com.claircore.iam.domain.model.aggregates.User;
import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.OAuthProvider;
import com.claircore.iam.domain.model.valueobjects.Password;
import com.claircore.iam.domain.model.valueobjects.UserStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserResourceFromEntityAssemblerTest {

    @Test
    void shouldMapUserEntityToResourceWhenUserHasIdentifier() {
        User user = User.rehydrate(
                UUID.randomUUID(),
                new EmailAddress("user@example.com"),
                new Password("encoded-password"),
                UserStatus.ACTIVE,
                OAuthProvider.MAIL,
                null
        ,
                null, null);

        var resource = UserResourceFromEntityAssembler.toResourceFromEntity(user);

        assertEquals(user.getId(), resource.id());
        assertEquals("user@example.com", resource.email());
    }
}
