package com.rimworldcraft.core.api.types;

import java.util.List;

/** Immutable sequence of actions. */
public record Plan(List<Action> actions, int totalCost, PlanStatus status) {
    /** Creates a validated plan. */
    public Plan {
        if (actions == null || totalCost < 0 || status == null) throw new IllegalArgumentException("Invalid plan");
        actions = List.copyOf(actions);
    }
}
