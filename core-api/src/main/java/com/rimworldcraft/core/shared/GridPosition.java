package com.rimworldcraft.core.shared;

import java.util.Objects;

/** Canonical platform-neutral grid coordinate. */
public record GridPosition(int x, int y, int z) {
    /** Returns a translated position. */
    public GridPosition add(int dx, int dy, int dz) { return new GridPosition(x + dx, y + dy, z + dz); }
    /** Returns Euclidean distance to another position. */
    public double distance(GridPosition other) {
        Objects.requireNonNull(other, "other");
        long dx = (long) x - other.x;
        long dy = (long) y - other.y;
        long dz = (long) z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    /** Converts this position to the current API compatibility type. */
    public com.rimworldcraft.core.api.types.GridPosition toApiType() { return new com.rimworldcraft.core.api.types.GridPosition(x, y, z); }
}
