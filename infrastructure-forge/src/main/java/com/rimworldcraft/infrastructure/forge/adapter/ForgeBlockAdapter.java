package com.rimworldcraft.infrastructure.forge.adapter;

import com.rimworldcraft.core.api.ports.IBlockWorldPort;
import com.rimworldcraft.core.api.types.BlockType;
import com.rimworldcraft.core.api.types.Position;
import java.util.Objects;

/** Forge block adapter seam; Minecraft objects stay in this infrastructure package. */
public final class ForgeBlockAdapter implements IBlockWorldPort {
    private final Object level;
    /** Creates an adapter for a Forge level instance. */
    public ForgeBlockAdapter(Object level) { this.level = Objects.requireNonNull(level); }
    /** Returns the platform-neutral block type. */
    @Override public BlockType getBlockType(Position pos) { Objects.requireNonNull(pos); return BlockType.UNKNOWN; }
    /** Sets a block through the future Forge mapping. */
    @Override public void setBlock(Position pos, BlockType type) { Objects.requireNonNull(pos); Objects.requireNonNull(type); throw new UnsupportedOperationException("Forge mapping not configured"); }
    /** Returns whether a block is air. */
    @Override public boolean isAir(Position pos) { return getBlockType(pos) == BlockType.AIR; }
    /** Returns whether a block is solid. */
    @Override public boolean isSolid(Position pos) { return getBlockType(pos) == BlockType.SOLID; }
}
