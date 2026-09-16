package com.claircore.billing.domain.model.commands;

import com.claircore.billing.domain.model.valueobjects.UserId;
import java.util.Objects;

public record InitializeUserPlanCommand(UserId userId) {
    public InitializeUserPlanCommand { Objects.requireNonNull(userId, "userId"); }
}
