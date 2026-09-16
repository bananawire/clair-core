package com.claircore.iam.infrastructure.persistence.jpa.assemblers;

import com.claircore.iam.domain.model.aggregates.User;
import com.claircore.iam.infrastructure.persistence.jpa.entities.UserPersistenceEntity;

public final class UserPersistenceAssembler {

    private UserPersistenceAssembler() {
    }

    public static User toDomainFromPersistence(UserPersistenceEntity entity) {
        if (entity == null) return null;
        return User.rehydrate(
                entity.getId(),
                entity.getEmail(),
                entity.getPassword(),
                entity.getStatus(),
                entity.getOauthProvider(),
                entity.getOauthSubject(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static UserPersistenceEntity toPersistenceFromDomain(User user) {
        if (user == null) return null;
        var entity = new UserPersistenceEntity();
        entity.setId(user.getId());
        entity.setEmail(user.getEmail());
        entity.setPassword(user.getPassword());
        entity.setStatus(user.getStatus());
        entity.setOauthProvider(user.getOauthProvider());
        entity.setOauthSubject(user.getOauthSubject());
        // Null for a user that has never been written; that is what marks the entity as new.
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());
        return entity;
    }
}
