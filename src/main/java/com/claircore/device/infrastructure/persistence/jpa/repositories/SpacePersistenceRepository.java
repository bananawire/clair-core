package com.claircore.device.infrastructure.persistence.jpa.repositories;

import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.infrastructure.persistence.jpa.entities.SpacePersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpacePersistenceRepository extends JpaRepository<SpacePersistenceEntity, UUID> {

    List<SpacePersistenceEntity> findByOrganizationId(UUID organizationId);

    int countByOrganizationId(UUID organizationId);

    int countByOwnerUserId(UserId ownerUserId);

    boolean existsByOrganizationId(UUID organizationId);

    boolean existsByIdAndOwnerUserId(UUID id, UserId ownerUserId);

    void deleteByOrganizationId(UUID organizationId);
}
