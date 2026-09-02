package com.rimworldcraft.core.goal;

import com.rimworldcraft.core.shared.CitizenId;
import java.util.Objects;
import java.util.UUID;

public record PlanFailure(UUID planId, CitizenId citizenId, String reason, int attempts) {
    public PlanFailure { Objects.requireNonNull(planId); Objects.requireNonNull(citizenId); if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason"); if (attempts < 0) throw new IllegalArgumentException("attempts"); }
}
