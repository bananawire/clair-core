package com.claircore.iam.application.queryservices;

import com.claircore.iam.domain.model.aggregates.User;
import com.claircore.iam.domain.model.queries.GetUserByEmailQuery;

import java.util.Optional;

public interface UserQueryService {
    Optional<User> handle(GetUserByEmailQuery query);
}
