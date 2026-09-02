package com.rimworldcraft.core.ports.driven;

import com.rimworldcraft.core.api.types.GameTick;

/** Supplies deterministic simulation time to application services. */
public interface ClockPort {
    /** Returns the current simulation tick. */
    GameTick currentTick();
}
