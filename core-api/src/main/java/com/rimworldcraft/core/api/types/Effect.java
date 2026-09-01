package com.rimworldcraft.core.api.types;

/** Effect applied by an AI action. */
@FunctionalInterface
public interface Effect {
    /** Applies the effect to a world state. */
    WorldState apply(WorldState state);
}
