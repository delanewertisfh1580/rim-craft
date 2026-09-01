package com.rimworldcraft.core.building;

import com.rimworldcraft.core.api.types.Position;
import java.util.Objects;
import java.util.UUID;
/** Immutable ghost block projection. */
public record GhostBlock(Position position, String blockType, int rotation, UUID buildOrderId, UUID placedBy) {
    /** Validates a ghost block. */
    public GhostBlock { Objects.requireNonNull(position); Objects.requireNonNull(blockType); Objects.requireNonNull(buildOrderId); Objects.requireNonNull(placedBy); if (rotation < 0 || rotation > 3) throw new IllegalArgumentException("rotation must be 0..3"); }
}
