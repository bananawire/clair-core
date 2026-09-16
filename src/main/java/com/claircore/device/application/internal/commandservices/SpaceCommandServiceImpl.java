package com.claircore.device.application.internal.commandservices;

import com.claircore.device.application.internal.outboundservices.acl.ExternalBillingService;
import com.claircore.device.domain.model.commands.CreateSpaceCommand;
import com.claircore.device.domain.model.commands.DeleteSpaceCommand;
import com.claircore.device.domain.model.commands.UpdateSpaceNameCommand;
import com.claircore.device.domain.model.aggregates.Organization;
import com.claircore.device.domain.model.aggregates.Space;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.device.application.commandservices.SpaceCommandService;
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
public class SpaceCommandServiceImpl implements SpaceCommandService {

    private final SpaceRepository spaceRepository;
    private final OrganizationRepository organizationRepository;
    private final DeviceAssignmentRepository deviceAssignmentRepository;
    private final ExternalBillingService externalBillingService;

    public SpaceCommandServiceImpl(
            SpaceRepository spaceRepository,
            OrganizationRepository organizationRepository,
            DeviceAssignmentRepository deviceAssignmentRepository,
            ExternalBillingService externalBillingService) {
        this.spaceRepository = spaceRepository;
        this.organizationRepository = organizationRepository;
        this.deviceAssignmentRepository = deviceAssignmentRepository;
        this.externalBillingService = externalBillingService;
    }

    @Override
    @Transactional
    public Space handle(CreateSpaceCommand command) {
        Organization org = organizationRepository
            .findById(command.organizationId())
            .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        if (!command.ownerUserId().equals(org.getOwnerUserId())) {
            throw new AccessDeniedException("Organization does not belong to user");
        }

        UUID userId = command.ownerUserId().userId();
        int currentCount = spaceRepository.countByOwnerUserId(command.ownerUserId());
        int maxAllowed = externalBillingService.getMaxSpaces(userId);

        if (currentCount >= maxAllowed) {
            throw new IllegalStateException(
                "Cannot create space. User has " + currentCount + " spaces, max allowed is " + maxAllowed
            );
        }

        Space space = new Space(
            command.name(),
            command.organizationId(),
            command.ownerUserId()
        );

        return spaceRepository.save(space);
    }

    @Override
    @Transactional
    public void handle(DeleteSpaceCommand command) {
        Space space = spaceRepository
            .findById(command.spaceId())
            .orElseThrow(() -> new IllegalArgumentException("Space not found"));
        requireOwner(space, command.userId());

        if (deviceAssignmentRepository.existsBySpaceId(command.spaceId())) {
            throw new IllegalStateException("Cannot delete space with devices. Remove all devices first.");
        }

        spaceRepository.deleteById(space.getId());
    }

    @Override
    @Transactional
    public void handle(UpdateSpaceNameCommand command) {
        Space space = spaceRepository
            .findById(command.spaceId())
            .orElseThrow(() -> new IllegalArgumentException("Space not found"));
        requireOwner(space, command.userId());

        space.updateName(command.name());
        spaceRepository.save(space);
    }

    private static void requireOwner(Space space, UserId userId) {
        if (!userId.equals(space.getOwnerUserId())) {
            throw new AccessDeniedException("Space does not belong to user");
        }
    }

    @Override
    public Optional<Space> findById(UUID id) {
        return spaceRepository.findById(id);
    }

    @Override
    public List<Space> findByOrganizationId(UUID organizationId) {
        return spaceRepository.findByOrganizationId(organizationId);
    }

    @Override
    public int countByOrganizationId(UUID organizationId) {
        return spaceRepository.countByOrganizationId(organizationId);
    }
}
