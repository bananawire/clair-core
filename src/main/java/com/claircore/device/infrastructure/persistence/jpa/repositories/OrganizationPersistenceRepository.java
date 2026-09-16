package com.claircore.device.infrastructure.persistence.jpa.repositories;

import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.infrastructure.persistence.jpa.entities.OrganizationPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrganizationPersistenceRepository extends JpaRepository<OrganizationPersistenceEntity, UUID> {

    List<OrganizationPersistenceEntity> findByOwnerUserId(UserId ownerUserId);

    int countByOwnerUserId(UserId ownerUserId);
}
