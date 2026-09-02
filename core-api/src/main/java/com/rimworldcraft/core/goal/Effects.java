package com.rimworldcraft.core.goal;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Pure effect application helper for planner transitions. */
public final class Effects {
    private Effects() { }

    public static Set<StateFact> apply(Set<StateFact> facts, ActionDefinition action) {
        Objects.requireNonNull(facts, "facts");
        Objects.requireNonNull(action, "action");
        Set<StateFact> result = new HashSet<>(facts);
        action.effects().forEach(effect -> result.removeIf(existing -> existing.key().equals(effect.key())));
        result.addAll(action.effects());
        return Set.copyOf(result);
    }
}
