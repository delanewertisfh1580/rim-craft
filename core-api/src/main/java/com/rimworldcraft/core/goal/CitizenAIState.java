package com.rimworldcraft.core.goal;

import com.rimworldcraft.core.shared.CitizenId;
import com.rimworldcraft.core.shared.WorldId;
import java.util.Objects;
import java.util.UUID;

/** Immutable, serializable decision state for one citizen; runtime handles are never stored. */
public record CitizenAIState(
        CitizenId citizenId,
        WorldId worldId,
        Goal goal,
        Plan plan,
        AIStatus status,
        int replans,
        int nextActionIndex,
        long actionStartedTick,
        int actionAttempts,
        long lastTick) {
    public CitizenAIState {
        Objects.requireNonNull(citizenId, "citizenId");
        Objects.requireNonNull(worldId, "worldId");
        Objects.requireNonNull(status, "status");
        if (replans < 0 || nextActionIndex < 0 || actionAttempts < 0 || lastTick < 0 || actionStartedTick < 0) {
            throw new IllegalArgumentException("invalid AI state");
        }
        if (plan != null && nextActionIndex > plan.actions().size()) {
            throw new IllegalArgumentException("action index is outside plan");
        }
        if (status == AIStatus.ACTIVE && (goal == null || plan == null)) {
            throw new IllegalArgumentException("active state requires goal and plan");
        }
    }

    /** Compatibility constructor for the initial Goal AI skeleton. */
    public CitizenAIState(CitizenId citizenId, Goal goal, Plan plan, int replans, long lastTick) {
        this(citizenId, new WorldId(new UUID(0L, 0L)), goal, plan,
                plan == null ? AIStatus.IDLE : AIStatus.ACTIVE, replans, 0, lastTick, 0, lastTick);
    }

    public boolean hasNextAction() {
        return plan != null && nextActionIndex < plan.actions().size();
    }

    public ActionDefinition nextAction() {
        if (!hasNextAction()) throw new IllegalStateException("no next action");
        return plan.actions().get(nextActionIndex);
    }

    public CitizenAIState advanceAction(long tick) {
        if (!hasNextAction()) throw new IllegalStateException("no next action");
        int next = nextActionIndex + 1;
        return new CitizenAIState(citizenId, worldId, goal, plan,
                next == plan.actions().size() ? AIStatus.IDLE : AIStatus.ACTIVE,
                replans, next, tick, 0, tick);
    }

    public CitizenAIState withActiveAction(long tick, int attempts) {
        return new CitizenAIState(citizenId, worldId, goal, plan, AIStatus.ACTIVE,
                replans, nextActionIndex, tick, attempts, tick);
    }

    public CitizenAIState suspended() {
        return new CitizenAIState(citizenId, worldId, goal, plan, AIStatus.SUSPENDED,
                replans, nextActionIndex, actionStartedTick, actionAttempts, lastTick);
    }
}
