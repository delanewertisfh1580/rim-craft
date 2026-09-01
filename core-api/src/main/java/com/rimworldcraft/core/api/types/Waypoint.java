package com.rimworldcraft.core.api.types;

/** Immutable route waypoint. */
public record Waypoint(
        Position position,
        boolean requiresBreaking,
        boolean requiresDoor) {
    /** Creates a waypoint. */
    public Waypoint {
        if (position == null) {
            throw new IllegalArgumentException("position must not be null");
        }
    }
}
