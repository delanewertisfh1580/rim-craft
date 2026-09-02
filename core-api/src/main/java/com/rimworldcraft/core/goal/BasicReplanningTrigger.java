package com.rimworldcraft.core.goal;

import java.util.Objects;

/** Replans only when observed facts or the selected goal materially change. */
public final class BasicReplanningTrigger implements ReplanningTrigger {
    @Override
    public boolean shouldReplan(WorldState previous, WorldState current, Plan plan) {
        if (previous == null || current == null || plan == null) return true;
        return !Objects.equals(previous.worldId(), current.worldId())
                || !Objects.equals(previous.citizenId(), current.citizenId())
                || !previous.facts().equals(current.facts())
                || (current.observedTick() - plan.createdTick()) >= 40;
    }
}
