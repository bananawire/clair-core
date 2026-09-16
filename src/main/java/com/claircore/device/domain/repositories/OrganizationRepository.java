package com.claircore.device.domain.repositories;

import com.claircore.device.domain.model.aggregates.Organization;
import com.claircore.device.domain.model.valueobjects.UserId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port for organization storage. Domain types only. */
public interface OrganizationRepository {

    Organization save(Organization organization);

    Optional<Organization> findById(UUID id);

    List<Organization> findByOwnerUserId(UserId ownerUserId);

    int countByOwnerUserId(UserId ownerUserId);

    void deleteById(UUID id);
}
