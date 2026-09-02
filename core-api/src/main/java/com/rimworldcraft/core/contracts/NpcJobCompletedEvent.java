package com.rimworldcraft.core.contracts;

import com.rimworldcraft.core.npc.domain.JobAttempt;
import com.rimworldcraft.core.shared.CitizenId;
import com.rimworldcraft.core.shared.WorldId;
import java.util.Objects;

/** Typed fact consumed by Colony or other contexts; it does not mutate inventory. */
public record NpcJobCompletedEvent(WorldId worldId, CitizenId citizenId, String jobType, JobAttempt attempt) {
    public NpcJobCompletedEvent {
        Objects.requireNonNull(worldId, "worldId"); Objects.requireNonNull(citizenId, "citizenId");
        if (jobType == null || jobType.isBlank()) throw new IllegalArgumentException("jobType must not be blank");
        Objects.requireNonNull(attempt, "attempt");
        if (attempt.status() != JobAttempt.Status.COMPLETED) throw new IllegalArgumentException("attempt must be completed");
    }
}
