package com.rimworldcraft.core.api.ports;

import com.rimworldcraft.core.api.types.GameTick;

/** Canonical deterministic time port for new code. */
public interface TimePort {
    /** Returns the current simulation tick. */
    GameTick currentTick();
}
