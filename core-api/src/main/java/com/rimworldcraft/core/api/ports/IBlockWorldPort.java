package com.rimworldcraft.core.api.ports;

import com.rimworldcraft.core.api.types.BlockType;
import com.rimworldcraft.core.api.types.Position;

/** Provides access to world blocks without exposing Minecraft types. */
public interface IBlockWorldPort {
    /** Returns the block type at a position. */
    BlockType getBlockType(Position pos);
    /** Sets the block type at a position. */
    void setBlock(Position pos, BlockType type);
    /** Returns whether a position contains air. */
    boolean isAir(Position pos);
    /** Returns whether a position contains a solid block. */
    boolean isSolid(Position pos);
}
