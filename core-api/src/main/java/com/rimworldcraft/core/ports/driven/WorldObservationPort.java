package com.rimworldcraft.core.ports.driven;

import com.rimworldcraft.core.api.types.BlockType;
import com.rimworldcraft.core.api.types.GridPosition;
import com.rimworldcraft.core.api.types.WorldId;

/** Driven port for platform-neutral world observations and block operations. */
public interface WorldObservationPort {
    /** Returns the block type at a world position. */
    BlockType blockAt(WorldId worldId, GridPosition position);
    /** Returns whether a position is empty. */
    boolean isAir(WorldId worldId, GridPosition position);
    /** Returns whether a position is solid. */
    boolean isSolid(WorldId worldId, GridPosition position);
}
