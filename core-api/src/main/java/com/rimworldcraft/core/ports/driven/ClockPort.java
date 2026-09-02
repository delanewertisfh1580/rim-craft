package com.rimworldcraft.core.ports.driven;

import com.rimworldcraft.core.shared.WorldId;
import com.rimworldcraft.core.shared.GameTick;

/** Supplies simulation time; domain code never reads system time. */
public interface ClockPort { GameTick currentTick(WorldId worldId); }
