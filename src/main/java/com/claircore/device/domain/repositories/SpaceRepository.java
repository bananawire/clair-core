package com.claircore.device.domain.repositories;

import com.claircore.device.domain.model.aggregates.Space;
import com.claircore.device.domain.model.valueobjects.UserId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port for space storage. Domain types only. */
public interface SpaceRepository {

    Space save(Space space);

    Optional<Space> findById(UUID id);

    List<Space> findByOrganizationId(UUID organizationId);

    int countByOrganizationId(UUID organizationId);

    int countByOwnerUserId(UserId ownerUserId);

    boolean existsByOrganizationId(UUID organizationId);

    boolean existsByIdAndOwnerUserId(UUID id, UserId ownerUserId);

    void deleteById(UUID id);

    void deleteByOrganizationId(UUID organizationId);
}
