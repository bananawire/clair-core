package com.claircore.analytics.application.commandservices;

import com.claircore.analytics.domain.model.commands.AggregateHourlySnapshotCommand;

public interface SnapshotAggregationCommandService {

    /** @return how many snapshots were written, one per device with telemetry in the window. */
    int handle(AggregateHourlySnapshotCommand command);
}
