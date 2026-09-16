package com.claircore.device.application.commandservices;

import com.claircore.device.domain.model.commands.AcknowledgeDeviceCommandCommand;
import com.claircore.device.domain.model.commands.CreateDeviceCommandCommand;
import com.claircore.device.domain.model.commands.DispatchPendingDeviceCommandsCommand;
import com.claircore.device.domain.model.aggregates.DeviceCommand;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceControlCommandService {
    DeviceCommand handle(CreateDeviceCommandCommand command);
    List<DeviceCommand> handle(DispatchPendingDeviceCommandsCommand command);
    DeviceCommand handle(AcknowledgeDeviceCommandCommand command);
    Optional<DeviceCommand> claimForEdge(UUID commandId, Instant leaseCutoff, Instant claimedAt);
}
