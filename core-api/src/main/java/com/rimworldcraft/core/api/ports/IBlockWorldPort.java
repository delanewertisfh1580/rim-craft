package com.rimworldcraft.core.api.ports;

import com.rimworldcraft.core.api.types.BlockType;
import com.rimworldcraft.core.api.types.Position;

/**
 * Legacy compatibility world port.
 *
 * @deprecated Use {@code com.rimworldcraft.core.ports.driven.WorldObservationPort}
 * with {@code WorldId} and {@code GridPosition}.
 */
@Deprecated(forRemoval = false)
public interface IBlockWorldPort {
    /** Returns the block type at a legacy position. */
    BlockType getBlockType(Position pos);
    /** Sets the block type at a legacy position. */
    void setBlock(Position pos, BlockType type);
    /** Returns whether a legacy position contains air. */
    boolean isAir(Position pos);
    /** Returns whether a legacy position is solid. */
    boolean isSolid(Position pos);
}
