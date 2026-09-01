package com.rimworldcraft.core.api.types;

/** Predicate used by AI goals and actions. */
@FunctionalInterface
public interface Condition {
    /** Evaluates the condition against a world state. */
    boolean test(WorldState state);
}
