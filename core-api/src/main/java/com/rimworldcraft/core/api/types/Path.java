package com.rimworldcraft.core.api.types;

import java.util.List;

/** Immutable route between positions. */
public record Path(List<Waypoint> waypoints, double length, long expectedTicks) {
    /** Creates an immutable path. */
    public Path {
        if (waypoints == null) throw new IllegalArgumentException("waypoints must not be null");
        waypoints = List.copyOf(waypoints);
    }
}
