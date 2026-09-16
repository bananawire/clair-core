package com.claircore.device.application.commandservices;

import com.claircore.device.domain.model.commands.CreateOrganizationCommand;
import com.claircore.device.domain.model.commands.DeleteOrganizationCommand;
import com.claircore.device.domain.model.commands.UpdateOrganizationNameCommand;
import com.claircore.device.domain.model.aggregates.Organization;
import com.claircore.device.domain.model.valueobjects.UserId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationCommandService {
    Organization handle(CreateOrganizationCommand command);
    void handle(DeleteOrganizationCommand command);
    void handle(UpdateOrganizationNameCommand command);
    Optional<Organization> findById(UUID id);
    List<Organization> findByOwnerUserId(UserId userId);
    int countByOwnerUserId(UserId userId);
}