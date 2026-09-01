package com.rimworldcraft.core.api.types;

/** Legacy compatibility position; new contracts should use {@link GridPosition}. */
public record Position(int x, int y, int z) {
    /** Converts this legacy value to the canonical position. */
    public GridPosition toGridPosition() { return new GridPosition(x, y, z); }
    /** Creates a legacy position from the canonical value. */
    public static Position from(GridPosition position) {
        return new Position(position.x(), position.y(), position.z());
    }
    /** Returns a translated position. */
    public Position add(int dx, int dy, int dz) { return new Position(x + dx, y + dy, z + dz); }
    /** Returns Euclidean distance to another position. */
    public double distance(Position other) {
        long dx = (long) x - other.x;
        long dy = (long) y - other.y;
        long dz = (long) z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
