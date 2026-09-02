package com.rimworldcraft.core.goal;

import com.rimworldcraft.core.api.types.GridPosition;
import com.rimworldcraft.core.shared.CitizenId;
import com.rimworldcraft.core.shared.WorldId;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable observation consumed by priority, planning, and execution services. */
public record WorldState(WorldId worldId, CitizenId citizenId, GridPosition position,
                         Optional<GridPosition> targetPosition, Set<StateFact> facts, long observedTick) {
    public WorldState {
        Objects.requireNonNull(worldId, "worldId");
        Objects.requireNonNull(citizenId, "citizenId");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(targetPosition, "targetPosition");
        Objects.requireNonNull(facts, "facts");
        targetPosition = targetPosition == null ? Optional.empty() : targetPosition;
        facts = Set.copyOf(facts);
        if (observedTick < 0) throw new IllegalArgumentException("tick must be >= 0");
    }

    /** Compatibility constructor for the original five-field record. */
    public WorldState(WorldId worldId, CitizenId citizenId, GridPosition position,
                      Set<StateFact> facts, long observedTick) {
        this(worldId, citizenId, position, Optional.empty(), facts, observedTick);
    }

    public boolean has(StateFact fact) { return facts.contains(Objects.requireNonNull(fact, "fact")); }
}
