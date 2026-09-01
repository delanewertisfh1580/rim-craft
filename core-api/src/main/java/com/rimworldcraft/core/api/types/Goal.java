package com.rimworldcraft.core.api.types;

import java.util.List;

/** Immutable citizen AI goal. */
public record Goal(GoalType type, int priority, List<Condition> preconditions, Action targetAction) {
    /** Creates a validated goal. */
    public Goal {
        if (type == null || priority < 0 || preconditions == null || targetAction == null) throw new IllegalArgumentException("Invalid goal");
        preconditions = List.copyOf(preconditions);
    }
}
