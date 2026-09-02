package com.rimworldcraft.core.npc.domain;

import com.rimworldcraft.core.shared.GridPosition;
import com.rimworldcraft.core.shared.WorldId;
import java.util.Objects;
import java.util.UUID;

/** Domain intent for work; execution is delegated through a port. */
public record JobAssignment(UUID jobId, String jobType, WorldId worldId, GridPosition target, long assignedAtTick) {
    public JobAssignment {
        Objects.requireNonNull(jobId, "jobId"); Objects.requireNonNull(worldId, "worldId");
        if (jobType == null || jobType.isBlank()) throw new IllegalArgumentException("jobType must not be blank");
        Objects.requireNonNull(target, "target");
        if (assignedAtTick < 0) throw new IllegalArgumentException("tick must be >= 0");
    }
}
