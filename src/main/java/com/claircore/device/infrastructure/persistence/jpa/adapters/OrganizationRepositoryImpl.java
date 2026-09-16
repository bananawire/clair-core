package com.claircore.device.infrastructure.persistence.jpa.adapters;

import com.claircore.device.domain.model.aggregates.Organization;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.domain.repositories.OrganizationRepository;
import com.claircore.device.infrastructure.persistence.jpa.assemblers.OrganizationPersistenceAssembler;
import com.claircore.device.infrastructure.persistence.jpa.repositories.OrganizationPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class OrganizationRepositoryImpl implements OrganizationRepository {

    private final OrganizationPersistenceRepository organizationPersistenceRepository;

    public OrganizationRepositoryImpl(OrganizationPersistenceRepository organizationPersistenceRepository) {
        this.organizationPersistenceRepository = organizationPersistenceRepository;
    }

    @Override
    public Organization save(Organization organization) {
        var saved = organizationPersistenceRepository.save(
                OrganizationPersistenceAssembler.toPersistenceFromDomain(organization));
        return OrganizationPersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public Optional<Organization> findById(UUID id) {
        return organizationPersistenceRepository.findById(id)
                .map(OrganizationPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<Organization> findByOwnerUserId(UserId ownerUserId) {
        return organizationPersistenceRepository.findByOwnerUserId(ownerUserId).stream()
                .map(OrganizationPersistenceAssembler::toDomainFromPersistence).toList();
    }

    @Override
    public int countByOwnerUserId(UserId ownerUserId) {
        return organizationPersistenceRepository.countByOwnerUserId(ownerUserId);
    }

    @Override
    public void deleteById(UUID id) {
        organizationPersistenceRepository.deleteById(id);
    }
}
