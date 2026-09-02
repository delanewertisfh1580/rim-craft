package com.rimworldcraft.core.building;

import com.rimworldcraft.core.api.types.GridPosition;
import java.util.Objects;
import java.util.UUID;

public record GhostBlock(GridPosition position,String blockType,int rotation,UUID buildOrderId,UUID placedBy) {
    public GhostBlock { Objects.requireNonNull(position); if(blockType==null||blockType.isBlank())throw new IllegalArgumentException("blockType");Objects.requireNonNull(buildOrderId);Objects.requireNonNull(placedBy);if(rotation<0||rotation>3)throw new IllegalArgumentException("rotation must be 0..3"); }
}
