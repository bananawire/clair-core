package com.claircore.device.infrastructure.persistence.jpa.adapters;

import com.claircore.device.domain.model.aggregates.Space;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.domain.repositories.SpaceRepository;
import com.claircore.device.infrastructure.persistence.jpa.assemblers.SpacePersistenceAssembler;
import com.claircore.device.infrastructure.persistence.jpa.repositories.SpacePersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SpaceRepositoryImpl implements SpaceRepository {

    private final SpacePersistenceRepository spacePersistenceRepository;

    public SpaceRepositoryImpl(SpacePersistenceRepository spacePersistenceRepository) {
        this.spacePersistenceRepository = spacePersistenceRepository;
    }

    @Override
    public Space save(Space space) {
        var saved = spacePersistenceRepository.save(SpacePersistenceAssembler.toPersistenceFromDomain(space));
        return SpacePersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public Optional<Space> findById(UUID id) {
        return spacePersistenceRepository.findById(id).map(SpacePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<Space> findByOrganizationId(UUID organizationId) {
        return spacePersistenceRepository.findByOrganizationId(organizationId).stream()
                .map(SpacePersistenceAssembler::toDomainFromPersistence).toList();
    }

    @Override
    public int countByOrganizationId(UUID organizationId) {
        return spacePersistenceRepository.countByOrganizationId(organizationId);
    }

    @Override
    public int countByOwnerUserId(UserId ownerUserId) {
        return spacePersistenceRepository.countByOwnerUserId(ownerUserId);
    }

    @Override
    public boolean existsByOrganizationId(UUID organizationId) {
        return spacePersistenceRepository.existsByOrganizationId(organizationId);
    }

    @Override
    public boolean existsByIdAndOwnerUserId(UUID id, UserId ownerUserId) {
        return spacePersistenceRepository.existsByIdAndOwnerUserId(id, ownerUserId);
    }

    @Override
    public void deleteById(UUID id) {
        spacePersistenceRepository.deleteById(id);
    }

    @Override
    public void deleteByOrganizationId(UUID organizationId) {
        spacePersistenceRepository.deleteByOrganizationId(organizationId);
    }
}
