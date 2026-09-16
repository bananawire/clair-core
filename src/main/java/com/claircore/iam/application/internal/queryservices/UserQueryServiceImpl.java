package com.claircore.iam.application.internal.queryservices;

import com.claircore.iam.application.queryservices.UserQueryService;
import com.claircore.iam.domain.model.aggregates.User;
import com.claircore.iam.domain.model.queries.GetUserByEmailQuery;
import com.claircore.iam.domain.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserQueryServiceImpl implements UserQueryService {

    private final UserRepository userRepository;

    public UserQueryServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> handle(GetUserByEmailQuery query) {
        return userRepository.findByEmail(query.email())
                .filter(User::isActive);
    }
}
