package com.rimworldcraft.core.api.types;

/** Movement capabilities used by pathfinding. */
public record PathfinderContext(double speed, boolean canJump, boolean canFly,
                                boolean canBreakBlocks, boolean canOpenDoors) {
    /** Creates a walking context. */
    public static PathfinderContext walking() {
        return new PathfinderContext(1.0, true, false, false, true);
    }
}
