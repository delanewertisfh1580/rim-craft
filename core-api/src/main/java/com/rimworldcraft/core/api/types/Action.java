package com.rimworldcraft.core.api.types;

import java.util.List;
import java.util.UUID;

/** Immutable executable AI action description. */
public record Action(UUID id, ActionType type, int duration,
                     List<Condition> preconditions, List<Effect> effects, int cost) {
    /** Creates a validated action. */
    public Action {
        if (id == null || type == null || duration < 0 || cost < 0 || preconditions == null || effects == null) {
            throw new IllegalArgumentException("Invalid action");
        }
        preconditions = List.copyOf(preconditions);
        effects = List.copyOf(effects);
    }
}
