package com.rimworldcraft.core.api.ports;

import com.rimworldcraft.core.api.types.GameTick;

/** Supplies deterministic simulation time to application and domain services. */
public interface ClockPort {
    /** Returns the current simulation tick. */
    GameTick currentTick();
}
