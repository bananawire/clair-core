package com.claircore.device.application.internal.commandservices;

import com.claircore.device.application.internal.outboundservices.acl.ExternalBillingService;
import com.claircore.device.domain.model.commands.CreateOrganizationCommand;
import com.claircore.device.domain.model.commands.DeleteOrganizationCommand;
import com.claircore.device.domain.model.commands.UpdateOrganizationNameCommand;
import com.claircore.device.domain.model.aggregates.Organization;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.application.commandservices.OrganizationCommandService;
import com.claircore.device.domain.repositories.DeviceAssignmentRepository;
import com.claircore.device.domain.repositories.OrganizationRepository;
import com.claircore.device.domain.repositories.SpaceRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrganizationCommandServiceImpl implements OrganizationCommandService {

    private final OrganizationRepository organizationRepository;
    private final SpaceRepository spaceRepository;
    private final DeviceAssignmentRepository deviceAssignmentRepository;
    private final ExternalBillingService externalBillingService;

    public OrganizationCommandServiceImpl(
            OrganizationRepository organizationRepository,
            SpaceRepository spaceRepository,
            DeviceAssignmentRepository deviceAssignmentRepository,
            ExternalBillingService externalBillingService) {
        this.organizationRepository = organizationRepository;
        this.spaceRepository = spaceRepository;
        this.deviceAssignmentRepository = deviceAssignmentRepository;
        this.externalBillingService = externalBillingService;
    }

    @Override
    @Transactional
    public Organization handle(CreateOrganizationCommand command) {
        UUID userId = command.ownerUserId().userId();

        int currentCount = organizationRepository.countByOwnerUserId(command.ownerUserId());
        int maxAllowed = externalBillingService.getMaxOrganizations(userId);

        if (currentCount >= maxAllowed) {
            throw new IllegalStateException(
                "Cannot create organization. User has " + currentCount + " organizations, max allowed is " + maxAllowed
            );
        }

        Organization organization = new Organization(
            command.name(),
            command.ownerUserId()
        );

        return organizationRepository.save(organization);
    }

    @Override
    @Transactional
    public void handle(DeleteOrganizationCommand command) {
        Organization organization = organizationRepository
            .findById(command.organizationId())
            .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        requireOwner(organization, command.userId());

        if (deviceAssignmentRepository.existsByOrganizationId(command.organizationId())) {
            throw new IllegalStateException(
                "Cannot delete organization with devices. Remove all devices first."
            );
        }

        spaceRepository.deleteByOrganizationId(command.organizationId());
        organizationRepository.deleteById(organization.getId());
    }

    @Override
    @Transactional
    public void handle(UpdateOrganizationNameCommand command) {
        Organization organization = organizationRepository
            .findById(command.organizationId())
            .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        requireOwner(organization, command.userId());

        organization.updateName(command.name());
        organizationRepository.save(organization);
    }

    private static void requireOwner(Organization organization, UserId userId) {
        if (!userId.equals(organization.getOwnerUserId())) {
            throw new AccessDeniedException("Organization does not belong to user");
        }
    }

    @Override
    public Optional<Organization> findById(UUID id) {
        return organizationRepository.findById(id);
    }

    @Override
    public List<Organization> findByOwnerUserId(UserId userId) {
        return organizationRepository.findByOwnerUserId(userId);
    }

    @Override
    public int countByOwnerUserId(UserId userId) {
        return organizationRepository.countByOwnerUserId(userId);
    }
}
