package com.claircore.iam.interfaces.rest.transform;

import com.claircore.iam.domain.model.aggregates.User;
import com.claircore.iam.interfaces.rest.resources.UserResource;

public class UserResourceFromEntityAssembler {
    public static UserResource toResourceFromEntity(User entity) {
        return new UserResource(
                entity.getId(),
                entity.getEmail().address()
        );
    }
}
