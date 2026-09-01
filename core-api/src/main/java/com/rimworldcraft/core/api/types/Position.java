package com.rimworldcraft.core.api.types;

/** Immutable three-dimensional world position. */
public record Position(int x, int y, int z) {
    /** Returns a translated position. */
    public Position add(int dx, int dy, int dz) { return new Position(x + dx, y + dy, z + dz); }
    /** Returns a position translated in the opposite direction. */
    public Position subtract(int dx, int dy, int dz) { return new Position(x - dx, y - dy, z - dz); }
    /** Returns Euclidean distance to another position. */
    public double distance(Position other) {
        long dx = (long) x - other.x;
        long dy = (long) y - other.y;
        long dz = (long) z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
