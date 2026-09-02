package com.rimworldcraft.core.building;

import com.rimworldcraft.core.api.types.GridPosition;

public interface PlacementValidator {
    boolean isWithinBounds(Blueprint blueprint, GridPosition origin, int maxWidth, int maxHeight, int maxDepth);
    boolean isCollisionFree(Blueprint blueprint, GridPosition origin);
    boolean isValidBlockType(String blockType);
}
