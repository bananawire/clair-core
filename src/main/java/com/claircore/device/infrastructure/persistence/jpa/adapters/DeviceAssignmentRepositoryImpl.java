package com.claircore.device.infrastructure.persistence.jpa.adapters;

import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.valueobjects.ClaimToken;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.domain.repositories.DeviceAssignmentRepository;
import com.claircore.device.infrastructure.persistence.jpa.assemblers.DeviceAssignmentPersistenceAssembler;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceAssignmentPersistenceRepository;
import com.claircore.shared.domain.model.PageResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DeviceAssignmentRepositoryImpl implements DeviceAssignmentRepository {

    private final DeviceAssignmentPersistenceRepository assignmentPersistenceRepository;

    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public DeviceAssignmentRepositoryImpl(DeviceAssignmentPersistenceRepository assignmentPersistenceRepository,
                                          org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.assignmentPersistenceRepository = assignmentPersistenceRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DeviceAssignment save(DeviceAssignment assignment) {
        var saved = assignmentPersistenceRepository.save(
                DeviceAssignmentPersistenceAssembler.toPersistenceFromDomain(assignment));
        return DeviceAssignmentPersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public Optional<DeviceAssignment> findById(UUID id) {
        return assignmentPersistenceRepository.findById(id)
                .map(DeviceAssignmentPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public PageResult<DeviceAssignment> findBySpaceId(UUID spaceId, int page, int size) {
        var found = assignmentPersistenceRepository.findBySpaceId(spaceId, PageRequest.of(page, size));
        return new PageResult<>(
                found.getContent().stream()
                        .map(DeviceAssignmentPersistenceAssembler::toDomainFromPersistence).toList(),
                page, size, found.getTotalElements());
    }

    @Override
    public long countBySpaceId(UUID spaceId) {
        return assignmentPersistenceRepository.countBySpaceId(spaceId);
    }

    @Override
    public boolean existsBySpaceId(UUID spaceId) {
        return assignmentPersistenceRepository.existsBySpaceId(spaceId);
    }

    @Override
    public Optional<DeviceAssignment> findByDeviceId(UUID deviceId) {
        return assignmentPersistenceRepository.findByDeviceId(deviceId)
                .map(DeviceAssignmentPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<DeviceAssignment> findByDeviceIdForUpdate(UUID deviceId) {
        return assignmentPersistenceRepository.findByDeviceIdForUpdate(deviceId)
                .map(DeviceAssignmentPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<DeviceAssignment> findByClaimToken(String claimToken) {
        return assignmentPersistenceRepository.findByClaimToken(new ClaimToken(claimToken))
                .map(DeviceAssignmentPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<DeviceAssignment> findByClaimTokenForUpdate(String claimToken) {
        return assignmentPersistenceRepository.findByClaimTokenForUpdate(new ClaimToken(claimToken))
                .map(DeviceAssignmentPersistenceAssembler::toDomainFromPersistence);
    }

    /**
     * A transaction-scoped advisory lock keyed by the owner. There is no owner row in this context
     * to lock, and the quota spans every space the owner has, so a row lock on any one of them would
     * not serialize the count. H2 (tests) has no advisory locks; there the method is a no-op.
     */
    @Override
    public void lockOwnerQuotaBoundary(UserId ownerUserId) {
        if (isPostgres()) {
            jdbcTemplate.queryForObject("SELECT pg_advisory_xact_lock(hashtext(?))", Object.class,
                    "device-quota:" + ownerUserId.userId());
        }
    }

    private boolean isPostgres() {
        var dataSource = jdbcTemplate.getDataSource();
        if (dataSource == null) return false;
        try (var connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName().toLowerCase().contains("postgres");
        } catch (java.sql.SQLException e) {
            return false;
        }
    }

    @Override
    public boolean existsByOrganizationId(UUID organizationId) {
        return assignmentPersistenceRepository.existsByOrganizationId(organizationId);
    }

    @Override
    public long countByOwnerUserId(UserId ownerUserId) {
        return assignmentPersistenceRepository.countByOwnerUserId(ownerUserId);
    }

    @Override
    public List<UUID> findDeviceIdsByOwnerUserId(UserId ownerUserId) {
        return assignmentPersistenceRepository.findDeviceIdsByOwnerUserId(ownerUserId);
    }

    @Override
    public boolean existsByDeviceIdAndOwnerUserId(UUID deviceId, UserId ownerUserId) {
        return assignmentPersistenceRepository.existsByDeviceIdAndOwnerUserId(deviceId, ownerUserId);
    }

    @Override
    public void deleteById(UUID id) {
        assignmentPersistenceRepository.deleteById(id);
    }
}
