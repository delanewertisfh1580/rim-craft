package com.rimworldcraft.core.goal;

import java.util.Collection;
import java.util.Objects;

/** Pure precondition evaluation; it never mutates the observed world state. */
public final class Preconditions {
    private Preconditions() { }

    public static boolean satisfiedBy(Collection<StateFact> required, WorldState state) {
        Objects.requireNonNull(required, "required");
        Objects.requireNonNull(state, "state");
        return required.stream().allMatch(state::has);
    }
}
